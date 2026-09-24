#!/usr/bin/env python3
"""Cloudstream __New şablon üreticisi; Windows, Python 3.10+, yalnızca stdlib.

python cloudstream_generator.py --help
Şablonun beklenen özellikleri bulunmazsa çıktı yayımlanmaz. Kotlin derleyicisi
değildir: desteklenmeyen sözdiziminde tahmin etmek yerine hata verir.
"""
from __future__ import annotations

import argparse
from dataclasses import dataclass
import ipaddress
from pathlib import Path
import re
import shutil
import sys
import tempfile
import unicodedata
from urllib.parse import quote, urlencode, urlsplit, urlunsplit


class GeneratorError(Exception):
    """Kullanıcının düzeltebileceği giriş veya şablon hatası."""


KEYWORDS = set('as break class continue do else false for fun if in interface is null object package return super this throw true try typealias typeof val var when while by catch constructor delegate dynamic field file finally get import init param property receiver set setparam where actual abstract annotation companion const crossinline data enum expect external final infix inline inner internal lateinit noinline open operator out override private protected public reified sealed suspend tailrec vararg value'.split())
DEVICES = {'CON', 'PRN', 'AUX', 'NUL', *(f'COM{i}' for i in range(1, 10)), *(f'LPT{i}' for i in range(1, 10))}
IDENTIFIER = re.compile(r'[A-Za-z_][A-Za-z0-9_]*\Z')
TYPES = {'movie': 'setOf(TvType.Movie)', 'tv': 'setOf(TvType.TvSeries)', 'both': 'setOf(TvType.Movie, TvType.TvSeries)'}


def ascii_name(value: str) -> str:
    value = value.translate(str.maketrans({'ı': 'i', 'İ': 'I', 'ş': 's', 'Ş': 'S', 'ğ': 'g', 'Ğ': 'G'}))
    return unicodedata.normalize('NFKD', value).encode('ascii', 'ignore').decode()


def class_name(value: str) -> str:
    parts = re.findall(r'[A-Za-z0-9]+', ascii_name(value))
    result = ''.join(part[:1].upper() + part[1:] for part in parts)
    if result and result[0].isdigit():
        result = 'Provider' + result
    if not result or len(result) > 80 or result.upper() in DEVICES or result.lower() in KEYWORDS:
        raise GeneratorError('Ad geçerli ve en fazla 80 karakterlik bir sınıf adına dönüştürülemedi.')
    return result


def package_name(value: str) -> str:
    result = ascii_name(value.strip()).lower()
    parts = result.split('.')
    if len(parts) == 1:
        parts.insert(0, 'com')
    if any(not IDENTIFIER.fullmatch(p) or p in KEYWORDS or p.upper() in DEVICES for p in parts):
        raise GeneratorError('Paket, noktayla ayrılmış geçerli tanımlayıcılardan oluşmalı: com.ornek')
    return '.'.join(parts)


def clean_text(value: str, field: str) -> str:
    value = value.strip()
    if not value or any(unicodedata.category(c) == 'Cc' for c in value):
        raise GeneratorError(f'{field} boş olamaz ve kontrol karakterleri içeremez.')
    return value


def normalize_url(value: str, *, base: bool = False) -> str:
    value = clean_text(value, 'URL')
    if '\\' in value or any(c.isspace() for c in value):
        raise GeneratorError('URL boşluk veya ters eğik çizgi içeremez.')
    if '://' not in value:
        value = 'https://' + value
    try:
        parsed = urlsplit(value)
        if parsed.scheme.lower() not in {'http', 'https'} or not parsed.hostname:
            raise ValueError('HTTP(S) ve sunucu adı gerekli')
        if parsed.username is not None or parsed.password is not None:
            raise ValueError('kullanıcı bilgisi içeren URL desteklenmiyor')
        host = parsed.hostname.encode('idna').decode('ascii').lower()
        if ':' in host:
            host = '[' + str(ipaddress.IPv6Address(host)) + ']'
        elif any(not re.fullmatch(r'[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?', label) for label in host.rstrip('.').split('.')):
            raise ValueError('sunucu adı geçersiz')
        port = parsed.port
        if base and (parsed.query or parsed.fragment):
            raise ValueError('ana URL sorgu veya fragment içeremez')
        path = quote(parsed.path, safe="/%:@!$&'()*+,;=-._~")
        if base:
            path = path.rstrip('/')
        return urlunsplit((parsed.scheme.lower(), host + (f':{port}' if port is not None else ''), path, quote(parsed.query, safe="%=&?/:@!$'()*+,;~-._"), quote(parsed.fragment, safe='%')))
    except (ValueError, UnicodeError) as exc:
        raise GeneratorError(f'Geçersiz URL: {exc}') from exc


def kotlin_string(value: str) -> str:
    """Kotlin/Groovy yerine yalnızca Kotlin DSL çıktısına literal yazar."""
    return '"' + value.replace('\\', '\\\\').replace('"', '\\"').replace('$', '\\$').replace('\n', '\\n').replace('\r', '\\r').replace('\t', '\\t') + '"'


def code_mask(source: str) -> str:
    """Yorum/string içeriğini konumları koruyarak gizler; iç içe yorum destekler.

    String interpolation ifadeleri de string ile birlikte maskelenir. Üreticinin
    düzenlediği bildirimler string içinde olamaz. Tüm grameri parse etmez.
    """
    chars = list(source)
    i = 0
    while i < len(source):
        start = i
        if source.startswith('//', i):
            end = source.find('\n', i)
            i = len(source) if end < 0 else end
        elif source.startswith('/*', i):
            depth = 1
            i += 2
            while i < len(source) and depth:
                if source.startswith('/*', i):
                    depth += 1
                    i += 2
                elif source.startswith('*/', i):
                    depth -= 1
                    i += 2
                else:
                    i += 1
            if depth:
                raise GeneratorError('Kapanmamış Kotlin yorumu.')
        elif source.startswith('"""', i):
            end = source.find('"""', i + 3)
            if end < 0:
                raise GeneratorError('Kapanmamış Kotlin raw string.')
            i = end + 3
        elif source[i] in '\"\'':
            delimiter = source[i]
            i += 1
            while i < len(source):
                if source[i] == '\\':
                    i += 2
                elif source[i] == delimiter:
                    i += 1
                    break
                else:
                    i += 1
            else:
                raise GeneratorError('Kapanmamış Kotlin string/char.')
        elif source[i] == '`':
            raise GeneratorError('Backtick tanımlayıcılar desteklenmiyor; şablonu sadeleştirin.')
        else:
            i += 1
            continue
        for j in range(start, min(i, len(source))):
            if chars[j] not in '\r\n':
                chars[j] = ' '
    return ''.join(chars)


def closing(mask: str, start: int) -> int:
    pairs = {'(': ')', '{': '}', '[': ']'}
    stack = []
    for i in range(start, len(mask)):
        c = mask[i]
        if c in pairs:
            stack.append(pairs[c])
        elif c in ')}]':
            if not stack or stack.pop() != c:
                raise GeneratorError('Eşleşmeyen Kotlin parantezi.')
            if not stack:
                return i
    raise GeneratorError('Kapanmamış Kotlin bloğu.')


def validate_balance(source: str) -> None:
    mask = code_mask(source)
    i = 0
    while i < len(mask):
        if mask[i] in '({[':
            i = closing(mask, i)
        elif mask[i] in ')}]':
            raise GeneratorError('Beklenmeyen kapanış parantezi.')
        i += 1


def replace_identifiers(source: str, mapping: dict[str, str]) -> str:
    mask = code_mask(source)
    edits = [(m.start(), m.end(), mapping[m.group()]) for m in re.finditer(r'\b[A-Za-z_][A-Za-z0-9_]*\b', mask) if m.group() in mapping]
    for start, end, replacement in reversed(edits):
        source = source[:start] + replacement + source[end:]
    return source


def set_property(source: str, name: str, value: str) -> str:
    mask = code_mask(source)
    # Anchored declaration/DSL assignment only, never a string or comment.
    pattern = rf'(?m)^[ \t]*(?:(?:override\s+)?(?:val|var)\s+)?{re.escape(name)}\s*(?::[ \t]*[\w<>?.]+[ \t]*)?='
    matches = list(re.finditer(pattern, mask))
    if len(matches) != 1:
        raise GeneratorError(f'{name}: tam bir bildirim bekleniyordu; bulunan: {len(matches)}')
    start = matches[0].end()
    while start < len(source) and source[start].isspace():
        start += 1
    # Explicit supported initializer forms; do not guess expression boundaries.
    literal = re.match(r'"""[\s\S]*?"""|"(?:\\.|[^"\\])*"', source[start:])
    call = re.match(r'(?:listOf|setOf)\s*\(', mask[start:])
    scalar = re.match(r'(?:true|false|[0-9]+)\b', mask[start:])
    if literal:
        end = start + literal.end()
    elif call:
        end = closing(mask, start + call.end() - 1) + 1
    elif scalar:
        end = start + scalar.end()
    else:
        raise GeneratorError(f'{name}: desteklenmeyen atama ifadesi.')
    tail = mask[end:].split('\n', 1)[0].strip()
    if tail and tail != ';':
        raise GeneratorError(f'{name}: aynı satırda ek ifade var; şablonu sadeleştirin.')
    return source[:start] + value + source[end:]


def function_span(source: str, name: str, *, required: bool = True) -> tuple[int, int] | None:
    mask = code_mask(source)
    pattern = rf'(?m)^[ \t]*(?:(?:override|private|public|protected|internal|suspend|inline|tailrec)\s+)*fun\s+(?:Element\s*\.\s*)?{re.escape(name)}\s*\('
    matches = list(re.finditer(pattern, mask))
    if not matches and not required:
        return None
    if len(matches) != 1:
        raise GeneratorError(f'{name}(): tam bir fonksiyon bekleniyordu; bulunan: {len(matches)}')
    match = matches[0]
    params_end = closing(mask, match.end()-1)
    body = params_end + 1
    while body < len(mask) and mask[body] not in '{=;':
        if re.match(r'\b(fun|class|override)\b', mask[body:]):
            raise GeneratorError(f'{name}(): blok gövdesi bulunamadı.')
        body += 1
    if body == len(mask) or mask[body] != '{':
        raise GeneratorError(f'{name}(): yalnızca {{ ... }} gövdeleri destekleniyor.')
    return match.start(), closing(mask, body) + 1


def replace_function(source: str, name: str, replacement: str, *, required: bool = True) -> str:
    span = function_span(source, name, required=required)
    if span is None:
        return source
    return source[:span[0]] + replacement.strip('\n') + source[span[1]:]


def package_of(source: str) -> str:
    matches = list(re.finditer(r'(?m)^\s*package[ \t]+([A-Za-z_][\w]*(?:\.[A-Za-z_][\w]*)*)[ \t]*;?', code_mask(source)))
    if len(matches) != 1:
        raise GeneratorError('Her Kotlin kaynak dosyasında tam bir package bildirimi gerekli.')
    return matches[0].group(1)


def rewrite_package_refs(source: str, old: str, new: str) -> str:
    mask = code_mask(source)
    pattern = rf'\b{re.escape(old)}\b(?![\w])'
    for match in reversed(list(re.finditer(pattern, mask))):
        source = source[:match.start()] + new + source[match.end():]
    return source


@dataclass(frozen=True)
class Config:
    template: Path
    output: Path
    display_name: str
    class_name: str
    package: str
    url: str
    author: str
    language: str
    description: str
    icon: str
    kind: str
    reuse_search: bool


def load_code(kind: str) -> str:
    """One shared metadata/episode skeleton prevents branch drift."""
    movie = response_code(False)
    tv = response_code(True)
    parts = ["    override suspend fun load(url: String): LoadResponse? {",
             "        val document = app.get(url).document", VARS_TEMPLATE.rstrip()]
    if kind == 'movie':
        parts.append(movie)
    elif kind == 'tv':
        parts.extend([EPISODES_TEMPLATE.rstrip(), tv])
    else:
        parts.append('        val isTv = document.select("div.episodios, div.season-list, table.episodes").isNotEmpty()')
        parts.append('        if (isTv) {')
        parts.extend('    ' + line if line else line for line in (EPISODES_TEMPLATE.rstrip() + '\n' + tv).splitlines())
        parts.append('        }')
        parts.append(movie)
    parts.append('    }')
    recommendation = RECOMMENDATION_TEMPLATE
    if kind == 'both':
        factory = RECOMMENDATION_BOTH
    else:
        factory_name = 'newTvSeriesSearchResponse' if kind == 'tv' else 'newMovieSearchResponse'
        tv_type = 'TvSeries' if kind == 'tv' else 'Movie'
        factory = f'        return {factory_name}(title, href, TvType.{tv_type}) {{\n            this.posterUrl = posterUrl\n        }}'
    parts.append(recommendation.replace('@@RESPONSE@@', factory))
    return '\n'.join(parts)


def response_code(series: bool) -> str:
    factory = 'newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes)' if series else 'newMovieLoadResponse(title, url, TvType.Movie, url)'
    return '        return ' + factory + ' {\n' + METADATA_TEMPLATE.rstrip() + '\n        }'


def normalize_search_types(source: str, kind: str) -> str:
    mask = code_mask(source)
    edits = []
    for match in re.finditer(r'\b(newMovieSearchResponse|newTvSeriesSearchResponse)\s*\(', mask):
        old = match.group(1)
        series = kind == 'tv' or (kind == 'both' and old == 'newTvSeriesSearchResponse')
        factory = 'newTvSeriesSearchResponse' if series else 'newMovieSearchResponse'
        edits.append((match.start(1), match.end(1), factory))
        end = closing(mask, match.end()-1)
        types = list(re.finditer(r'\bTvType\.(?:NSFW|Movie|TvSeries)\b', mask[match.end():end]))
        if len(types) > 1:
            raise GeneratorError('Arama yanıtında birden fazla TvType ifadesi var; şablonu sadeleştirin.')
        for token in types:
            edits.append((match.end()+token.start(), match.end()+token.end(), 'TvType.TvSeries' if series else 'TvType.Movie'))
    for start, end, value in sorted(edits, reverse=True):
        source = source[:start] + value + source[end:]
    return source


def transform_provider(source: str, cfg: Config) -> str:
    source = normalize_search_types(source, cfg.kind)
    for name, value in {'mainUrl': kotlin_string(cfg.url), 'name': kotlin_string(cfg.display_name), 'lang': kotlin_string(cfg.language), 'supportedTypes': TYPES[cfg.kind]}.items():
        source = set_property(source, name, value)
    source = replace_function(source, 'toRecommendationResult', '', required=False)
    source = replace_function(source, 'load', load_code(cfg.kind))
    if cfg.reuse_search:
        function_span(source, 'toMainPageResult')
        span = function_span(source, 'search')
        assert span is not None
        search = source[span[0]:span[1]]
        if not re.search(r'\btoSearchResult\s*\(', code_mask(search)):
            raise GeneratorError('search(): toSearchResult() çağrısı bulunamadı; otomatik paylaşım uygulanamadı.')
        source = source[:span[0]] + replace_identifiers(search, {'toSearchResult': 'toMainPageResult'}) + source[span[1]:]
        helper = function_span(source, 'toSearchResult', required=False)
        if helper:
            rest = source[:helper[0]] + source[helper[1]:]
            # Remove only when no other caller remains.
            if not re.search(r'\btoSearchResult\b', code_mask(rest)):
                source = rest
    imports = ['com.lagradost.cloudstream3.*', 'com.lagradost.cloudstream3.utils.*', 'org.jsoup.nodes.Element', 'com.lagradost.cloudstream3.LoadResponse.Companion.addActors', 'com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer']
    existing = set(re.findall(r'(?m)^\s*import\s+([\w.*]+)\s*$', code_mask(source)))
    needed = [f'import {item}' for item in imports if item not in existing]
    if needed:
        match = re.search(r'(?m)^\s*package[^\r\n]+', code_mask(source))
        assert match is not None
        source = source[:match.end()] + '\n\n' + '\n'.join(needed) + source[match.end():]
    return source


def read_source(path: Path) -> tuple[str, str, bool]:
    data = path.read_bytes()
    try:
        text = data.decode('utf-8-sig')
    except UnicodeDecodeError as exc:
        raise GeneratorError(f'UTF-8 kaynak bekleniyor: {path}') from exc
    newline = '\r\n' if '\r\n' in text else '\n'
    return text.replace('\r\n', '\n'), newline, data.startswith(b'\xef\xbb\xbf')


def write_source(path: Path, source: str, newline: str, bom: bool) -> None:
    validate_balance(source)
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes((b'\xef\xbb\xbf' if bom else b'') + source.replace('\n', newline).encode('utf-8'))


def transform_tree(stage: Path, cfg: Config) -> None:
    root = stage / 'src' / 'main' / 'kotlin'
    files = sorted(root.rglob('*.kt'))
    if not files:
        raise GeneratorError('src/main/kotlin altında Kotlin kaynağı yok.')
    sources = {p: read_source(p) for p in files}
    providers = []
    for path, (source, _, _) in sources.items():
        for match in re.finditer(r'\bclass\s+(\w+)\s*:\s*MainAPI\s*\(', code_mask(source)):
            providers.append((path, match.group(1)))
    if len(providers) != 1:
        raise GeneratorError('Şablonda tam bir class X : MainAPI() bekleniyor.')
    provider, old_class = providers[0]
    old_package = package_of(sources[provider][0])
    mapping = {old_class: cfg.class_name, old_class + 'Plugin': cfg.class_name + 'Plugin'}
    plans = []
    for path, (source, newline, bom) in sources.items():
        package = package_of(source)
        source = replace_identifiers(source, mapping)
        source = rewrite_package_refs(source, old_package, cfg.package)
        if path == provider:
            source = transform_provider(source, cfg)
        if package == old_package or package.startswith(old_package + '.'):
            new_package = cfg.package + package[len(old_package):]
            target = root.joinpath(*new_package.split('.'), mapping.get(path.stem, path.stem) + path.suffix)
        else:
            target = path.with_name(mapping.get(path.stem, path.stem) + path.suffix)
        plans.append((path, target, source, newline, bom))
    destinations = [str(p[1].relative_to(stage)).casefold() for p in plans]
    if len(destinations) != len(set(destinations)):
        raise GeneratorError('Yeniden adlandırma Windows dosya adı çakışması oluşturuyor.')
    old_paths = {p[0] for p in plans}
    for _, target, *_ in plans:
        if target.exists() and target not in old_paths:
            raise GeneratorError(f'Hedef dosya zaten var: {target}')
    # Read/plan every source first; removes are confined to the disposable copy.
    for old, *_ in plans:
        old.unlink()
    for _, target, source, newline, bom in plans:
        write_source(target, source, newline, bom)
    build = stage / 'build.gradle.kts'
    if not build.is_file():
        raise GeneratorError('Şablonda build.gradle.kts gerekli (Groovy DSL desteklenmiyor).')
    source, newline, bom = read_source(build)
    for name, value in {'authors': f'listOf({kotlin_string(cfg.author)})', 'language': kotlin_string(cfg.language), 'description': kotlin_string(cfg.description), 'iconUrl': kotlin_string(cfg.icon)}.items():
        source = set_property(source, name, value)
    # Cloudstream metadata uses string names, not TvType expressions.
    if re.search(r'(?m)^\s*tvTypes\s*=', code_mask(source)):
        values = {'movie': '"Movie"', 'tv': '"TvSeries"', 'both': '"Movie", "TvSeries"'}
        source = set_property(source, 'tvTypes', 'listOf(' + values[cfg.kind] + ')')
    source = replace_identifiers(source, mapping)
    write_source(build, source, newline, bom)
    settings = stage / 'settings.gradle.kts'
    if settings.exists():
        source, newline, bom = read_source(settings)
        source = set_property(source, 'rootProject.name', kotlin_string(cfg.class_name))
        write_source(settings, source, newline, bom)


def is_within(path: Path, parent: Path) -> bool:
    return path == parent or parent in path.parents


def check_tree(template: Path) -> None:
    # Reject links/junctions, including Windows reparse points (Python 3.10).
    import stat
    for path in [template, *template.rglob('*')]:
        info = path.lstat()
        if path.is_symlink() or getattr(info, 'st_file_attributes', 0) & getattr(stat, 'FILE_ATTRIBUTE_REPARSE_POINT', 0x400):
            raise GeneratorError(f'Şablonda sembolik bağlantı/junction desteklenmiyor: {path}')


def generate(cfg: Config) -> Path:
    if cfg.class_name != class_name(cfg.class_name) or cfg.package != package_name(cfg.package) or cfg.kind not in TYPES:
        raise GeneratorError('Config içinde normalleştirilmiş sınıf/paket ve geçerli tür gerekli.')
    template = cfg.template.resolve(strict=True)
    output = cfg.output.resolve()
    target = output / cfg.class_name
    if not template.is_dir():
        raise GeneratorError('Şablon bir klasör olmalı.')
    if is_within(output, template) or is_within(template, target):
        raise GeneratorError('Şablon ve çıktı klasörleri iç içe olamaz.')
    if target.exists() or target.is_symlink():
        raise GeneratorError(f'Hedef zaten var; üzerine yazılmadı: {target}')
    check_tree(template)
    output.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(prefix='.cloudstream-', dir=output) as temp:
        stage = Path(temp) / cfg.class_name
        shutil.copytree(template, stage, ignore=shutil.ignore_patterns('.git', '.gradle', 'build', '__pycache__'), symlinks=True)
        check_tree(stage)
        transform_tree(stage, cfg)
        # mkdir reserves the final name atomically; never overwrite user data.
        target.mkdir()
        try:
            shutil.copytree(stage, target, dirs_exist_ok=True, symlinks=True)
        except BaseException:
            # Only this run's newly created target is rolled back; preserve error.
            shutil.rmtree(target)
            raise
    return target


def parse_args(argv: list[str] | None = None) -> Config:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument('name', nargs='?', help='Görünen eklenti adı')
    parser.add_argument('url', nargs='?', help='Site ana URL adresi')
    parser.add_argument('--template', type=Path, default=Path('__New'))
    parser.add_argument('--output-dir', type=Path, default=Path.cwd())
    parser.add_argument('--class-name', help='İsteğe bağlı sınıf/proje adı')
    parser.add_argument('--package', default='com.kraptor')
    parser.add_argument('--author', default='kraptor')
    parser.add_argument('--language', default='en')
    parser.add_argument('--description')
    parser.add_argument('--icon')
    parser.add_argument('--type', dest='kind', choices=['movie', 'tv', 'both', '1', '2', '3'], default='both')
    parser.add_argument('--reuse-main-parser', action='store_true', help='search içinde ana sayfa ayrıştırıcısını kullan')
    args = parser.parse_args(argv)
    if args.name is None or args.url is None:
        if not sys.stdin.isatty():
            parser.error('Etkileşimsiz kullanımda name ve url zorunludur.')
        args.name = args.name or input('Ad: ')
        args.url = args.url or input('URL: ')
    name = clean_text(args.name, 'Ad')
    cls = class_name(args.class_name or name)
    package = package_name(args.package)
    url = normalize_url(args.url, base=True)
    language = args.language.strip().lower()
    if not re.fullmatch(r'[a-z]{2,3}(?:-[a-z0-9]{2,8})*', language):
        raise GeneratorError('Dil kodu geçersiz; örnek: tr, en, pt-br.')
    icon = normalize_url(args.icon) if args.icon else 'https://www.google.com/s2/favicons?' + urlencode({'sz': '64', 'domain': urlsplit(url).hostname})
    kind = {'1': 'movie', '2': 'tv', '3': 'both'}.get(args.kind, args.kind)
    return Config(args.template, args.output_dir, name, cls, package, url, clean_text(args.author.lstrip('@'), 'Yazar'), language, clean_text(args.description or f'{name} eklentisi.', 'Açıklama'), icon, kind, args.reuse_main_parser)


def main(argv: list[str] | None = None) -> int:
    try:
        cfg = parse_args(argv)
        target = generate(cfg)
        print(f'Tamamlandı: {target}\nSınıf/proje: {cfg.class_name}\nPaket: {cfg.package}\nTür: {cfg.kind}')
        print('CSS seçicilerini siteye uyarlayın ve projenin Gradle derlemesini çalıştırın.')
        return 0
    except (GeneratorError, OSError) as exc:
        print(f'Hata: {exc}', file=sys.stderr)
        return 1
    except (KeyboardInterrupt, EOFError):
        print('\nİşlem iptal edildi.', file=sys.stderr)
        return 130


# Siteye özgü başlangıç seçicileri; gerçek sağlayıcıya göre düzenlenmelidir.
# TV/Both tanıma kuralları ve bölüm seçicileri birlikte değiştirilmelidir.
VARS_TEMPLATE = r'''
        val title = document.selectFirst("h1")?.text()?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val poster = fixUrlNull(document.selectFirst("meta[property=og:image]")?.attr("content")?.takeIf { it.isNotBlank() })
        val description = document.selectFirst("meta[property=og:description]")?.attr("content")?.trim()
        val year = document.selectFirst("div.extra span.C a")?.text()?.trim()?.toIntOrNull()
        val tags = document.select("div.sgeneros a").map { it.text().trim() }.filter { it.isNotBlank() }.distinct()
        val scoreText = document.selectFirst("span.dt_rating_vgs")?.text()?.trim()
        val duration = document.selectFirst("span.runtime")?.text()?.trim()?.split(" ")?.firstOrNull()?.toIntOrNull()
        val recommendations = document.select("div.srelacionados article").mapNotNull { it.toRecommendationResult() }
        val actors = document.select("span.valor a").map { it.text().trim() }.filter { it.isNotBlank() }.distinct().map { Actor(it) }
        val trailer = document.selectFirst("iframe[src*=youtube.com/embed/]")?.attr("src")?.takeIf { it.isNotBlank() }?.let { fixUrl(it) }
'''

EPISODES_TEMPLATE = r'''
        val episodes = document.select("div.episodios li, div.season-list a, table.episodes tr").mapNotNull { ep ->
            val link = if (ep.tagName() == "a") ep else ep.selectFirst("a") ?: return@mapNotNull null
            val rawUrl = link.attr("href").trim().takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val epUrl = fixUrlNull(rawUrl) ?: return@mapNotNull null
            newEpisode(epUrl) {
                this.name = link.text().trim().ifBlank { "Bölüm" }
                this.season = ep.selectFirst(".se-t, .season")?.text()?.trim()?.toIntOrNull()
                this.episode = ep.selectFirst(".num-ep, .episode")?.text()?.trim()?.toIntOrNull()
            }
        }
'''

METADATA_TEMPLATE = r'''
            this.posterUrl = poster
            this.plot = description
            this.year = year
            this.tags = tags
            this.score = Score.from10(scoreText)
            this.duration = duration
            this.recommendations = recommendations
            addActors(actors)
            addTrailer(trailer)
'''

RECOMMENDATION_TEMPLATE = r'''
    private fun Element.toRecommendationResult(): SearchResponse? {
        val link = selectFirst("a") ?: return null
        val title = listOfNotNull(
            link.attr("title"), selectFirst("img")?.attr("alt"), link.text()
        ).firstOrNull { it.isNotBlank() }?.trim() ?: return null
        val rawUrl = link.attr("href").trim().takeIf { it.isNotBlank() } ?: return null
        val href = fixUrlNull(rawUrl) ?: return null
        val image = selectFirst("img")
        val posterUrl = fixUrlNull(image?.attr("data-src")?.takeIf { it.isNotBlank() }
            ?: image?.attr("src")?.takeIf { it.isNotBlank() })
@@RESPONSE@@
    }
'''

RECOMMENDATION_BOTH = r'''        val marker = selectFirst(".type")?.text()?.trim()?.lowercase()
        val isTv = selectFirst(".episodios, .serie-tag") != null || marker in setOf("tv", "series", "tv series", "dizi")
        return if (isTv) {
            newTvSeriesSearchResponse(title, href, TvType.TvSeries) {
                this.posterUrl = posterUrl
            }
        } else {
            newMovieSearchResponse(title, href, TvType.Movie) {
                this.posterUrl = posterUrl
            }
        }'''


if __name__ == '__main__':
    raise SystemExit(main())
