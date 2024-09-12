package com.appsbay.chineseclassicalliteratural.Model;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;

public class BookStore {
    public static BookStore shared = new BookStore();

    ArrayList<Book> booksChineseSimple = new ArrayList<Book>() {{
// 四书五经
        add(new Book("论语", "论语", "", "论语简体_json", BookType.siShuWuJing));
        add(new Book("孟子", "孟子", "佚名", "孟子_json", BookType.siShuWuJing));
        add(new Book("大学", "大学", "儒家经典", "大学简体_json", BookType.siShuWuJing));
        add(new Book("中庸简体", "中庸", "儒家经典", "中庸简体_json", BookType.siShuWuJing));
        add(new Book("尚书", "尚书", "儒家经典", "尚书_json", BookType.siShuWuJing));
        add(new Book("左传", "左传", "左丘明", "左传", BookType.siShuWuJing));
        add(new Book("礼记", "礼记", "儒家经典", "礼记", BookType.siShuWuJing));
        add(new Book("周易", "周易", "周文王", "周易", BookType.siShuWuJing));
// 诸子百家: + 论语 孟子
        add(new Book("孙子兵法", "孙子兵法", "孙武", "孙子兵法简体_json", BookType.zhuZiBaiJia));
        add(new Book("道德经", "道德经", "老子", "道德经_json", BookType.zhuZiBaiJia));
        add(new Book("庄子", "庄子内篇", "佚名", "庄子内篇_json", BookType.zhuZiBaiJia));
        add(new Book("庄子外篇", "庄子外篇", "佚名", "庄子外篇", BookType.zhuZiBaiJia));
        add(new Book("庄子杂篇", "庄子杂篇", "佚名", "庄子杂篇", BookType.zhuZiBaiJia));
        add(new Book("韩非子", "韩非子", "韩非", "韩非子", BookType.zhuZiBaiJia));
        add(new Book("荀子", "荀子", "荀况", "荀子", BookType.zhuZiBaiJia));
        add(new Book("淮南子", "淮南子", "刘安", "淮南子", BookType.zhuZiBaiJia));
        add(new Book("商君书", "商君书", "商鞅", "商君书", BookType.zhuZiBaiJia));
        add(new Book("列子", "列子", "列御寇子", "列子", BookType.zhuZiBaiJia));
        add(new Book("墨子", "墨子", "墨子", "墨子", BookType.zhuZiBaiJia));
        add(new Book("文子", "文子", "文子", "文子", BookType.zhuZiBaiJia));
        add(new Book("尉缭子", "尉缭子", "", "尉缭子", BookType.zhuZiBaiJia));
        add(new Book("公孙龙子", "公孙龙子", "公孙龙", "公孙龙子", BookType.zhuZiBaiJia));
        add(new Book("吕氏春秋", "吕氏春秋", "吕不韦", "吕氏春秋", BookType.zhuZiBaiJia));
        add(new Book("关尹子", "关尹子", "尹喜", "关尹子", BookType.zhuZiBaiJia));
        add(new Book("鹖冠子", "鹖冠子", "鹖冠子", "鹖冠子", BookType.zhuZiBaiJia));
// 经典小说
        add(new Book("东周列国志", "东周列国志", "余邵鱼、冯梦龙", "东周列国志简体_json", BookType.novel));
        add(new Book("喻世明言简体", "喻世明言", "冯梦龙", "喻世明言简体_json", BookType.novel));
        add(new Book("桃花扇", "桃花扇", "孔尚任", "桃花扇简体_json", BookType.novel));
        add(new Book("金石缘", "金石缘", "佚名", "金石缘简体_json", BookType.novel));
        add(new Book("儒林外史简体", "儒林外史", "吴敬梓", "儒林外史简体_json", BookType.novel));
        add(new Book("封神演义简体", "封神演义", "许仲琳", "封神演义简体_json", BookType.novel));
        add(new Book("镜花缘", "镜花缘", "李汝珍", "镜花缘_json", BookType.novel));
        add(new Book("平妖传", "平妖传", "罗贯中，冯梦龙", "平妖传", BookType.novel));
        add(new Book("初刻拍案惊奇", "初刻拍案惊奇", "凌濛初", "初刻拍案惊奇", BookType.novel));
        add(new Book("二刻拍案驚奇", "二刻拍案惊奇", "凌濛初", "二刻拍案惊奇", BookType.novel));
        add(new Book("隋炀帝艳史", "隋炀帝艳史", "齐东野人", "隋炀帝艳史", BookType.novel));
        add(new Book("醒世恒言", "醒世恒言", "冯梦龙", "醒世恒言", BookType.novel));
        add(new Book("隋唐演义", "隋唐演义", "褚人获", "隋唐演义", BookType.novel));
// 其他
        add(new Book("古文观止上", "古文观止上", "吴楚材、吴调侯", "古文观止上简体_json", BookType.other));
        add(new Book("古文观止下", "古文观止下", "吴楚材、吴调侯", "古文观止下简体_json", BookType.other));
        add(new Book("菜根譚", "菜根谭", "佚名", "菜根谭_json", BookType.other));
        add(new Book("山海經", "山海经", "佚名", "山海经_json", BookType.other));
        add(new Book("冰鉴", "冰鉴", "曾国藩", "冰鉴_json", BookType.other));
        add(new Book("洛神赋", "洛神赋", "曹植", "洛神赋简体_json", BookType.other));
        add(new Book("孝经", "孝经", "佚名", "孝经简体_json", BookType.other));
        add(new Book("老残游记", "老残游记", "刘鹗", "老残游记", BookType.other));
        add(new Book("文心雕龙", "文心雕龙", "刘勰", "文心雕龙", BookType.other));
        add(new Book("梦溪笔谈", "梦溪笔谈", "沈括", "梦溪笔谈", BookType.other));
        add(new Book("千家诗", "千家诗", "", "千家诗", BookType.other));
        add(new Book("浮生六记", "浮生六记", "沈复", "浮生六记", BookType.other));
        add(new Book("颜氏家训", "颜氏家训", "颜之推", "颜氏家训", BookType.other));
        add(new Book("大唐西域记", "大唐西域记", "玄奘，辩机", "大唐西域记", BookType.other));
    }};

    ArrayList<Book> booksChineseTraditional = new ArrayList<Book>() {{
        // 四书五经
        add(new Book("論語", "論語", "", "論語繁体_json", BookType.siShuWuJing_Fan));
        add(new Book("孟子", "孟子 (繁體)", "佚名", "孟子繁体_json", BookType.siShuWuJing_Fan));
        add(new Book("大學", "大學", "儒家經典", "大學繁体_json", BookType.siShuWuJing_Fan));
        add(new Book("中庸繁体", "中庸 (繁體)", "儒家經典", "中庸繁体_json", BookType.siShuWuJing_Fan));
        add(new Book("尚書", "尚書", "儒家經典", "尚書繁体_json", BookType.siShuWuJing_Fan));
        add(new Book("左传", "左傳", "左丘明", "左傳", BookType.siShuWuJing_Fan));
        add(new Book("礼记", "禮記", "儒家經典", "禮記", BookType.siShuWuJing_Fan));
        add(new Book("周易", "周易 (繁體)", "周文王", "周易_f", BookType.siShuWuJing_Fan));
// 诸子百家: + 论语 孟子
        add(new Book("孫子兵法", "孫子兵法", "孫武", "孫子兵法繁体_json", BookType.zhuZiBaiJia_Fan));
        add(new Book("道德經繁体", "道德經", "老子", "道德經繁体_json", BookType.zhuZiBaiJia_Fan));
        add(new Book("庄子", "莊子內篇", "莊子", "莊子內篇_json", BookType.zhuZiBaiJia_Fan));
        add(new Book("庄子外篇", "莊子外篇", "佚名", "莊子外篇", BookType.zhuZiBaiJia_Fan));
        add(new Book("庄子外篇", "莊子雜篇", "佚名", "莊子雜篇", BookType.zhuZiBaiJia_Fan));
        add(new Book("韩非子", "韓非子", "韓非", "韓非子", BookType.zhuZiBaiJia_Fan));
        add(new Book("荀子", "荀子 (繁體)", "荀況", "荀子_f", BookType.zhuZiBaiJia_Fan));
        add(new Book("淮南子", "淮南子 (繁體)", "劉安", "淮南子_f", BookType.zhuZiBaiJia_Fan));
        add(new Book("商君书", "商君書", "商鞅", "商君書", BookType.zhuZiBaiJia_Fan));
        add(new Book("列子", "列子", "列禦寇子", "列子_f", BookType.zhuZiBaiJia_Fan));
        add(new Book("墨子", "墨子 (繁體)", "墨子", "墨子_f", BookType.zhuZiBaiJia_Fan));
        add(new Book("文子", "文子 (繁體)", "文子", "文子_f", BookType.zhuZiBaiJia_Fan));
        add(new Book("尉缭子", "尉繚子", "", "尉缭子_f", BookType.zhuZiBaiJia_Fan));
        add(new Book("公孙龙子", "公孫龍子", "公孫龍", "公孙龙子_f", BookType.zhuZiBaiJia_Fan));
        add(new Book("吕氏春秋", "呂氏春秋", "呂不韋", "呂氏春秋_f", BookType.zhuZiBaiJia_Fan));
        add(new Book("关尹子", "關尹子", "尹喜", "關尹子_f", BookType.zhuZiBaiJia_Fan));
        add(new Book("鹖冠子", "鶡冠子", "鶡冠子", "鶡冠子_f", BookType.zhuZiBaiJia_Fan));
// 经典小说
        add(new Book("東周列國志", "東周列國志", "余邵鱼、馮夢龍", "東周列國志繁体_json", BookType.novel_Fan));
        add(new Book("喻世明言繁体", "喻世明言 (繁體)", "馮夢龍", "喻世明言繁体_json", BookType.novel_Fan));
        add(new Book("桃花扇", "桃花扇 (繁體)", "孔尚任", "桃花扇繁体_json", BookType.novel_Fan));
        add(new Book("金石緣", "金石緣", "佚名", "金石緣繁体_json", BookType.novel_Fan));
        add(new Book("儒林外史繁体", "儒林外史 (繁體)", "吳敬梓", "儒林外史繁体_json", BookType.novel_Fan));
        add(new Book("封神演義繁体", "封神演義", "許仲琳", "封神演義繁体_json", BookType.novel_Fan));
        add(new Book("鏡花緣繁体", "鏡花緣", "李汝珍", "鏡花緣繁体_json", BookType.novel_Fan));
        add(new Book("平妖传", "平妖傳", "羅貫中，馮夢龍", "平妖傳", BookType.novel_Fan));
        add(new Book("初刻拍案惊奇", "初刻拍案驚奇", "凌濛初", "初刻拍案惊奇_f", BookType.novel_Fan));
        add(new Book("二刻拍案驚奇", "二刻拍案驚奇", "凌濛初", "二刻拍案惊奇_f", BookType.novel_Fan));
        add(new Book("隋炀帝艳史", "隋煬帝豔史", "齊東野人", "隋炀帝艳史_f", BookType.novel_Fan));
        add(new Book("醒世恒言", "醒世恆言", "馮夢龍", "醒世恒言_f", BookType.novel_Fan));
        add(new Book("隋唐演义", "隋唐演義", "褚人獲", "隋唐演義_f", BookType.novel_Fan));
// 其他
        add(new Book("古文觀止上", "古文觀止上", "吳楚材、吳調侯", "古文觀止上繁体_json", BookType.other_Fan));
        add(new Book("古文觀止下", "古文觀止下", "吳楚材、吳調侯", "古文觀止下繁体_json", BookType.other_Fan));
        add(new Book("菜根譚", "菜根譚", "佚名", "菜根譚_json", BookType.other_Fan));
        add(new Book("山海經", "山海經", "佚名", "山海經_json", BookType.other_Fan));
        add(new Book("冰鉴", "冰鑑", "曾國藩", "冰鑑_json", BookType.other_Fan));
        add(new Book("洛神賦", "洛神賦", "曹植", "洛神賦繁体_json", BookType.other_Fan));
        add(new Book("孝經", "孝經", "佚名", "孝經繁体_json", BookType.other_Fan));
        add(new Book("老残游记", "老殘遊記", "劉鶚", "老殘遊記", BookType.other_Fan));
        add(new Book("文心雕龙", "文心雕龍", "劉勰", "文心雕龙_f", BookType.other_Fan));
        add(new Book("梦溪笔谈", "夢溪筆談", "沈括", "梦溪笔谈_f", BookType.other_Fan));
        add(new Book("千家诗", "千家詩", "", "千家诗_f", BookType.other_Fan));
        add(new Book("浮生六记", "浮生六記", "沈复", "浮生六记_f", BookType.other_Fan));
        add(new Book("颜氏家训", "顏氏家訓", "颜之推", "颜氏家训_f", BookType.other_Fan));
        add(new Book("大唐西域记", "大唐西域記", "玄奘，辯機", "大唐西域記_f", BookType.other_Fan));
    }};

    public ArrayList<Book> getBooks(Context context) {
        ArrayList<Book> books;

        SharedPreferences preferences = context.getSharedPreferences("Language Preference", Context.MODE_PRIVATE);
        int language = preferences.getInt("language", 0);

        if (language == 0) {
            books = BookStore.shared.booksChineseSimple;
        } else {
            books = BookStore.shared.booksChineseTraditional;
        }
        return books;
    }

}
