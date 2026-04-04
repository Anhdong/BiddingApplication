package com.uet.BiddingApplication.DAO.Interface;

import com.uet.BiddingApplication.Model.Item;

import java.util.List;

public interface IItemDAO {
    public boolean insertItem(Item item);
    public boolean updateItem(Item item);
    public boolean deleteItem(String itemId);
    public Item getItemById(String itemId);
    public List<Item> getItemsBySellerId(String sellerId);
    public List<Item> getItemsByIds(List<String> itemIds);
}
