package com.abin.mallchat.common.common.utils.chain;

import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import com.abin.mallchat.common.common.utils.FutureUtils;
import com.abin.mallchat.common.common.utils.discover.domain.UrlInfo;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.data.util.Pair;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Description: 链接处理工厂
 * Author: achao
 * Date: 2023/7/6 9:12
 */
@Slf4j
public abstract class FactoryUrlHandler extends UrlHandler{

    //链接识别的正则
    private static final Pattern PATTERN = Pattern.compile("((http|https)://)?(www.)?([\\w_-]+(?:(?:\\.[\\w_-]+)+))([\\w.,@?^=%&:/~+#-]*[\\w@?^=%&/~+#-])?");

    @Override
    @Nullable
    public Map<String, UrlInfo> getUrlContentMap(String content) {

        if (StrUtil.isBlank(content)) {
            return new HashMap<>();
        }
        List<String> matchList = ReUtil.findAll(PATTERN, content, 0);

        //并行请求
        List<CompletableFuture<Pair<String, UrlInfo>>> futures = matchList.stream().map(match -> CompletableFuture.supplyAsync(() -> {
            UrlInfo urlInfo = getContent(match);
            return Objects.isNull(urlInfo) ? null : Pair.of(match, urlInfo);
        })).collect(Collectors.toList());
        CompletableFuture<List<Pair<String, UrlInfo>>> future = FutureUtils.sequenceNonNull(futures);
        //结果组装
        return future.join().stream().collect(Collectors.toMap(Pair::getFirst, Pair::getSecond, (a, b) -> a));
    }

    private UrlInfo getContent(String url){
        url = !StrUtil.startWith(url, "http") ? "http://" + url : url;
        Document document = getUrlDocument(url);
        return UrlInfo.builder()
                .title(getTitle(document))
                .description(getDescription(document))
                .image(getImage(url,document)).build();
    }

    protected Document getUrlDocument(String matchUrl) {
        try {
            Connection connect = Jsoup.connect(matchUrl);
            connect.timeout(2000);
            return connect.get();
        } catch (Exception e) {
            log.error("find error:url:{}", matchUrl, e);
        }
        return null;
    }

    /**
     * 获取链接的标题
     * @param document
     * @return
     */
    @Nullable
    abstract String getTitle(Document document);

    /**
     * 获取链接的描述
     * @param document
     * @return
     */
    @Nullable
    abstract String getDescription(Document document);

    /**
     * 获取链接的LOGO
     * @param document
     * @return
     */
    @Nullable
    abstract String getImage(String url, Document document);

}
