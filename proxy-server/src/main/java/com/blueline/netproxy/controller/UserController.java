package com.blueline.netproxy.controller;

import com.blueline.netproxy.modle.Result;
import com.blueline.netproxy.modle.SuccessResult;
import com.blueline.netproxy.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Baili
 */
@RestController
public class UserController {

    private final IUserService userService;

    @Autowired
    public UserController(IUserService userService) {
        this.userService = userService;
    }

    @GetMapping("/user")
    public Result get(@RequestParam("user") String user) {
        return new SuccessResult(user);
    }
}
