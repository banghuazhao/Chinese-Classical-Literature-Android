# Catalog maintenance

Run `python3 scripts/validate_catalog.py` after editing catalog or chapter assets. Gradle also runs it during `:app:check`. It verifies both script variants, introductions, author bios, chapter content, cover files, and the chapter index exceptions used by shared reading progress.

Run `python3 scripts/generate_textless_covers.py` to recreate the 48 vector based PNG covers. The script requires `rsvg-convert` and optionally Pillow for PNG compression. The other 11 illustrated covers were inspected for printed text and are retained. All cover PNGs are shared by simplified and traditional editions.

The missing 神奇 chapter in both `梦溪笔谈` files was restored from the public domain [Wikisource transcription of 卷二十](https://zh.wikisource.org/wiki/%E5%A4%A2%E6%BA%AA%E7%AD%86%E8%AB%87/%E5%8D%B720). The simplified copy was converted with OpenCC.
