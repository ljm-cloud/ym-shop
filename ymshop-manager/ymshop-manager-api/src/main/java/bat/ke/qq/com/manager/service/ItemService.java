package bat.ke.qq.com.manager.service;

import bat.ke.qq.com.manager.dto.ItemDto;
import bat.ke.qq.com.common.pojo.DataTablesResult;
import bat.ke.qq.com.manager.pojo.TbItem;
import freemarker.template.TemplateException;

import java.io.IOException;


/**
 * @author bat.ke.qq.com
 * @date 2017/7/29
 */
public interface ItemService {

    /**
     * 通过ID获取商品包含详情
     * @param itemId
     * @return
     */
    ItemDto getItemById(Long itemId);

    /**
     * 通过ID获取不包含详情
     * @param id
     * @return
     */
    TbItem getNormalItemById(Long id);

    /**
     * 分页搜索排序获取商品列表
     * @param draw
     * @param start
     * @param length
     * @param cid
     * @param search
     * @param orderCol
     * @param orderDir
     * @return
     */
    DataTablesResult getItemList(int draw, int start, int length, int cid,
                                 String search, String orderCol, String orderDir);

    /**
     * 多条件查询获取商品列表
     * @param draw
     * @param start
     * @param length
     * @param cid
     * @param search
     * @param minDate
     * @param maxDate
     * @param orderCol
     * @param orderDir
     * @return
     */
    DataTablesResult getItemSearchList(int draw,int start,int length,int cid,
                                       String search,String minDate,String maxDate,
                                       String orderCol,String orderDir);

    /**
     * 获取商品总数
     * @return
     */
    DataTablesResult getAllItemCount();

    /**
     * 修改商品状态
     * @param id
     * @param state
     * @return
     */
    TbItem alertItemState(Long id,Integer state);

    /**
     * 彻底删除商品
     * @param id
     * @return
     */
    int deleteItem(Long id);

    /**
     * 添加商品
     * @param itemDto
     * @return
     */
    TbItem addItem(ItemDto itemDto);

    /**
     * 更新商品
     * @param id
     * @param itemDto
     * @return
     */
    TbItem updateItem(Long id,ItemDto itemDto);
    /**
     * 静态化商品详情页
     * @param id
     * @return
     */
    String toStatic(Long id);
}
