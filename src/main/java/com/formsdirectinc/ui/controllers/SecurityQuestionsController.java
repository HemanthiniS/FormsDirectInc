package com.formsdirectinc.ui.controllers;

import com.formsdirectinc.dao.CustomerSignup;
import com.formsdirectinc.services.account.AccountDelegate;
import com.formsdirectinc.tenant.TenantContextHolder;
import com.formsdirectinc.ui.auth.Authenticator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@Controller
@RequestMapping("/securityQuestion.do")
public class SecurityQuestionsController {

	@Autowired
	protected Authenticator authenticator;
	
	@Autowired
	protected AccountDelegate accountDelegate;
	
    public static final String ACTION_TO_FREEAPPLICATION = "freeapplication.do";
    public static final String SECURITY_QUESTION_VIEW = "security_question";
    public static final String ACTION_REDIRECT_TO_LOGIN = "redirect:/login.do";

    @RequestMapping(method = RequestMethod.GET)
    public String showForm(HttpServletRequest request,
	    HttpServletResponse response, HttpSession session, Model model) {

	if (!authenticator.signedIn(request, response, session)) {
	    return ACTION_REDIRECT_TO_LOGIN + "?next="
		    + ACTION_TO_FREEAPPLICATION;
	}

	return  SECURITY_QUESTION_VIEW;
    }

    @RequestMapping(method = RequestMethod.POST)
    public String updateSecurityQuestion(HttpServletRequest request,
	    HttpServletResponse response, HttpSession session,
	    @RequestParam("hintQuestion") Integer hintQuestion,
	    @RequestParam("hintAnswer") String hintAnswer,
	    @RequestParam("upsell") String upsell,
	    @RequestParam(value = "nextPage", required = false) String next) {

	CustomerSignup customerSignup = (CustomerSignup) session
		.getAttribute("customerSignup");
	session.setAttribute("upsell", upsell);
	customerSignup.setHintQuestion(hintQuestion);
	customerSignup.setHintAnswer(hintAnswer);
	accountDelegate.updateProfile(customerSignup);
	if (next != null) {
	    return next;
	}
	return SECURITY_QUESTION_VIEW;

    }
}
