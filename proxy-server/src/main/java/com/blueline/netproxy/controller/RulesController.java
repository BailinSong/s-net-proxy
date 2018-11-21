package com.blueline.netproxy.controller;

import com.blueline.netproxy.mode.RuleMapping;
import com.blueline.netproxy.modle.Result;
import com.blueline.netproxy.modle.SuccessResult;
import com.blueline.netproxy.service.IUserService;
import com.blueline.netproxy.service.RuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @author Baili
 */
@RestController
public class RulesController {

    @Autowired
    RuleService ruleService;

    @Autowired
    IUserService userService;

    @GetMapping("/rules")
    public Result getRules(@RequestParam("user") String user) {
        return new SuccessResult(ruleService.getProxyInfo(user));
    }

    @GetMapping("/rules/{id}")
    public Result getRules(@PathVariable("id") Integer id, @RequestParam("user") String user) {

        return new SuccessResult(ruleService.getProxyInfo(user).get(id));

    }

    @PostMapping("/rules")
    public Result createRules(@RequestParam("user") String user, @RequestBody RuleMapping ruleMapping) {
        ruleService.putProxyInfo(user, ruleMapping);
        return new SuccessResult(ruleService.getProxyInfo(user).get(ruleMapping.getId()));
    }

    @PutMapping("/rules")
    public Result putRules(@RequestParam("user") String user, @RequestBody RuleMapping ruleMapping) {
        return createRules(user, ruleMapping);
    }

    @DeleteMapping("/rules/{id}")
    public Result deleteRules(@PathVariable("id") Integer id, @RequestParam("user") String user) {

        ruleService.deleteProxyInfo(user, id);
        return new SuccessResult(id);

    }
}
