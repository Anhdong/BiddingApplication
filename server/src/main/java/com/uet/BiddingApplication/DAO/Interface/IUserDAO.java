package com.uet.BiddingApplication.DAO.Interface;

import com.uet.BiddingApplication.Model.User;

import java.util.List;

public interface IUserDAO {
    public User findByUsername(String name);
    public boolean insertUser(User user);
    public User findByEmail(String email);
    public User findById(String userId);
    public boolean updateStatus(String userId, boolean isActive);
    public boolean updateProfile(User user);
    public boolean changePassword(String userId, String newHashedPassword);
    public List<User> getAllUsers();
    public List<User> searchUsers(String keyword, String role, Boolean status);

}
