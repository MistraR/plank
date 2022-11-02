package com.mistra.plank.controller;

import com.mistra.plank.common.exception.FieldInputException;
import com.mistra.plank.model.entity.User;
import com.mistra.plank.model.vo.CommonResponse;
import com.mistra.plank.model.vo.UserVo;
import com.mistra.plank.service.UserService;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("user")
public class UserController extends BaseController {

    @Autowired
    private UserService userService;

    @PostMapping("login")
    public UserVo login(String username, String password) {
        if (!StringUtils.hasLength(username) || !StringUtils.hasLength(password)) {
            FieldInputException e = new FieldInputException();
            e.addError("user", "username and password could not be null");
            throw e;
        }
        User user = userService.login(username, password);
        if (user == null) {
            FieldInputException e = new FieldInputException();
            e.addError("user", "username or password is error");
            throw e;
        }
        String token = UUID.randomUUID().toString().replace("-", "");
        userService.putToSession(user, token);

        UserVo userVo = new UserVo();
        userVo.setToken(token);
        userVo.setEmail(user.getEmail());
        userVo.setMobile(user.getMobile());
        userVo.setName(user.getName());
        userVo.setUsername(user.getUsername());
        userVo.setToken(token);
        return userVo;
    }

    @PostMapping("updatePassword")
    public CommonResponse updatePassword(String oldPassword, String password, String password2) {
        if (!StringUtils.hasLength(oldPassword) || !StringUtils.hasLength(password) || !StringUtils.hasLength(password2)) {
            FieldInputException e = new FieldInputException();
            e.addError("user", "old password and password could not be null");
            throw e;
        }
        if (!password.equals(password2)) {
            FieldInputException e = new FieldInputException();
            e.addError("user", "confirmed password and new password do not match");
            throw e;
        }
        int userId = getUserId();
        User user = userService.getById(userId);
        if (!user.getPassword().equals(DigestUtils.md5Hex(oldPassword))) {
            FieldInputException e = new FieldInputException();
            e.addError("user", "old password is error");
            throw e;
        }
        user.setPassword(DigestUtils.md5Hex(password));
        userService.update(user);
        return CommonResponse.buildResponse("success");
    }

}
