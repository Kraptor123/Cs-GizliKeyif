import os
import sys
import re

def main():
    # En azından eklenti adı girilmiş mi kontrol et
    if len(sys.argv) < 2:
        print("Kullanım: python new.py <EklentiAdı> [SiteAdresi]")
        print("Örnek 1: python new.py YeniEklenti")
        print("Örnek 2: python new.py YeniEklenti google.com")
        print("Örnek 3: python new.py -YeniEklenti -https://google.com")
        return

    target_name = sys.argv[1]

    # 2. argüman olarak URL girilmişse al, yoksa None yap
    target_url = sys.argv[2] if len(sys.argv) > 2 else None

    # Argümanların başındaki tire (-) işaretlerini temizle
    if target_name.startswith('-'):
        target_name = target_name[1:]

    if target_url and target_url.startswith('-'):
        target_url = target_url[1:]

    if target_name == "__New":
        print("Hata: __New şablon klasörünü değiştiremezsiniz!")
        return

    target_dir = os.path.join(os.getcwd(), target_name)

    if not os.path.exists(target_dir):
        print(f"Hata: '{target_name}' adında bir klasör bulunamadı.")
        print(f"Lütfen önce terminalde 'cp -r __New {target_name}' komutunu çalıştırın.")
        return

    print(f"'{target_name}' klasöründeki dosyalar güncelleniyor...")

    # URL'leri formatla
    main_url_val = None
    icon_url_val = None
    if target_url:
        # Eğer adres http veya https ile başlamıyorsa, https:// ekle
        if not target_url.startswith("http://") and not target_url.startswith("https://"):
            target_url = "https://" + target_url

        main_url_val = target_url
        icon_url_val = f"https://www.google.com/s2/favicons?sz=64&domain={target_url}"

    # 1. Dosya ve klasör isimlerini değiştir
    for root, dirs, files in os.walk(target_dir, topdown=False):
        for file_name in files:
            if "New" in file_name:
                old_file_path = os.path.join(root, file_name)
                new_file_name = file_name.replace("New", target_name)
                new_file_path = os.path.join(root, new_file_name)
                os.rename(old_file_path, new_file_path)

        for dir_name in dirs:
            if "New" in dir_name:
                old_dir_path = os.path.join(root, dir_name)
                new_dir_name = dir_name.replace("New", target_name)
                new_dir_path = os.path.join(root, new_dir_name)
                os.rename(old_dir_path, new_dir_path)

    # 2. Dosya içeriklerini güncelle (İsim ve URL'ler)
    for root, dirs, files in os.walk(target_dir):
        for file_name in files:
            file_path = os.path.join(root, file_name)

            try:
                with open(file_path, 'r', encoding='utf-8') as f:
                    content = f.read()

                new_content = content
                content_changed = False

                # "New" isimlerini değiştir
                if "New" in new_content:
                    new_content = new_content.replace("New", target_name)
                    content_changed = True

                # URL'ler verilmişse Regex ile değiştir
                if main_url_val:
                    # mainUrl = "..." kısmını bul ve içini yeni URL ile değiştir
                    replaced_content = re.sub(r'(mainUrl\s*=\s*)".*?"', rf'\g<1>"{main_url_val}"', new_content)
                    if replaced_content != new_content:
                        new_content = replaced_content
                        content_changed = True

                    # iconUrl = "..." kısmını bul ve içini yeni ikon URL'si ile değiştir
                    replaced_content = re.sub(r'(iconUrl\s*=\s*)".*?"', rf'\g<1>"{icon_url_val}"', new_content)
                    if replaced_content != new_content:
                        new_content = replaced_content
                        content_changed = True

                # Sadece bir değişiklik yapıldıysa dosyayı kaydet
                if content_changed:
                    with open(file_path, 'w', encoding='utf-8') as f:
                        f.write(new_content)
                    print(f"Güncellendi: {os.path.basename(file_path)}")

            except Exception as e:
                pass

    print(f"\nBaşarılı! Eklenti '{target_name}' olarak ayarlandı.")
    if main_url_val:
        print(f"Ana URL: {main_url_val}")
        print(f"İkon URL: {icon_url_val}")

if __name__ == "__main__":
    main()