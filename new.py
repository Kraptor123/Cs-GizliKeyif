import os
import sys

def main():
    # Argüman verilip verilmediğini kontrol et
    if len(sys.argv) < 2:
        print("Kullanım: python new.py <EklentiAdı>")
        print("Örnek: python new.py YeniEklenti")
        return

    target_name = sys.argv[1]

    # Eğer alışkanlıktan "-YeniEklenti" yazarsan başındaki tireyi temizle
    if target_name.startswith('-'):
        target_name = target_name[1:]

    # Güvenlik önlemi: Şablon klasörüne dokunmayı engelle
    if target_name == "__New":
        print("Hata: __New şablon klasörünü değiştiremezsiniz!")
        return

    # İşlem yapılacak klasörün yolu
    target_dir = os.path.join(os.getcwd(), target_name)

    if not os.path.exists(target_dir):
        print(f"Hata: '{target_name}' adında bir klasör bulunamadı.")
        print(f"Lütfen önce terminalde 'cp -r __New {target_name}' komutunu çalıştırın.")
        return

    print(f"'{target_name}' klasöründeki dosyalar güncelleniyor...")

    # 1. Önce dosya ve klasör isimlerini güncelle (İçten dışa doğru taramak yolları bozmamak için önemli)
    for root, dirs, files in os.walk(target_dir, topdown=False):
        # Dosya isimlerini değiştir
        for file_name in files:
            if "New" in file_name:
                old_file_path = os.path.join(root, file_name)
                new_file_name = file_name.replace("New", target_name)
                new_file_path = os.path.join(root, new_file_name)
                os.rename(old_file_path, new_file_path)
                print(f"İsim güncellendi: {file_name} -> {new_file_name}")

        # Klasör isimlerini değiştir (Eğer klasör adında da "New" geçiyorsa)
        for dir_name in dirs:
            if "New" in dir_name:
                old_dir_path = os.path.join(root, dir_name)
                new_dir_name = dir_name.replace("New", target_name)
                new_dir_path = os.path.join(root, new_dir_name)
                os.rename(old_dir_path, new_dir_path)

    # 2. Dosyaların içeriklerindeki "New" yazılarını değiştir
    for root, dirs, files in os.walk(target_dir):
        for file_name in files:
            file_path = os.path.join(root, file_name)

            try:
                # Dosyayı oku
                with open(file_path, 'r', encoding='utf-8') as f:
                    content = f.read()

                # Eğer "New" kelimesi varsa değiştir ve kaydet
                if "New" in content:
                    new_content = content.replace("New", target_name)
                    with open(file_path, 'w', encoding='utf-8') as f:
                        f.write(new_content)
                    print(f"İçerik güncellendi: {os.path.basename(file_path)}")

            except Exception as e:
                # İkili (binary) dosyalar veya okuma/yazma izni olmayan dosyaları atla
                pass

    print(f"\nBaşarılı! Tüm 'New' ibareleri '{target_name}' olarak değiştirildi.")

if __name__ == "__main__":
    main()