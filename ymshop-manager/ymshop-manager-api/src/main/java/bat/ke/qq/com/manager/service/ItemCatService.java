package bat.ke.qq.com.manager.service;

import bat.ke.qq.com.common.pojo.ZTreeNode;
import bat.ke.qq.com.manager.pojo.TbItemCat;

import java.util.List;

/**
 * @author bat.ke.qq.com
 * @date 2017/8/2
 */
public interface ItemCatService {

    /**
     * 通过id获取
     * @param id
     * @return
     */
    TbItemCat getItemCatById(Long id);

    /**
     * 获得分类树
     * @param parentId
     * @return
     */
    List<ZTreeNode> getItemCatList(int parentId);

    /**
     * 添加分类
     * @param tbItemCat
     * @return
     */
    int addItemCat(TbItemCat tbItemCat);

    /**
     * 编辑分类
     * @param tbItemCat
     * @return
     */
    int updateItemCat(TbItemCat tbItemCat);

    /**
     * 删除单个分类
     * @param id
     */
    void deleteItemCat(Long id);

    /**
     * 递归删除
     * @param id
     */
    void deleteZTree(Long id);
}
