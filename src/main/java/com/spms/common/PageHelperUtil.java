package com.spms.common;

import com.ccsp.common.core.web.page.PageDomain;
import com.ccsp.common.core.web.page.TableSupport;

import java.util.List;

/**
 * @author Kong
 * @date 2023/2/10
 * @dec 描述 非数据库查询到的数据进行分页
 */
public class PageHelperUtil {
    private PageHelperUtil() {
        throw new IllegalStateException("Utility class");
    }
    public static <T> List<T> pageHelper(List<T> list) {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        //所选页码
        int pageNum = pageDomain.getPageNum();
        //每页数量
        int pageSize = pageDomain.getPageSize();
        if (list.size() <= 1) {
            return list;
        }
        int startIndex = (pageNum - 1) * pageSize;
        int endIndex = Math.min(startIndex + pageSize, list.size());
        if(endIndex<startIndex){
            startIndex=endIndex-pageSize;
        }
        return list.subList(startIndex, endIndex);
    }
}
