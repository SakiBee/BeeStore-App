package com.sakibee.controller;

import com.sakibee.model.Category;
import com.sakibee.model.Product;
import com.sakibee.model.User;
import com.sakibee.service.CartService;
import com.sakibee.service.CategoryService;
import com.sakibee.service.ProductService;
import com.sakibee.service.UserService;
import com.sakibee.utils.CommonUtil;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.List;
import java.util.UUID;

@Controller
public class HomeController {
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private ProductService productService;
    @Autowired
    private UserService userService;
    @Autowired
    private CommonUtil commonUtil;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private CartService cartService;

    @ModelAttribute
    public void getUserDatails(Principal p, Model m) {
        if(p != null) {
            String email = p.getName();
            User user = userService.getUserByEmail(email);
            m.addAttribute("user", user);
            Integer productCount = cartService.getCartProductCount(user.getId());
            m.addAttribute("productCount",productCount);
        }
        List<Category> category = categoryService.getAllActiveCategory();
        m.addAttribute("category", category);

    }

    @GetMapping("/")
    public String index(Model m) {
        List<Category> categories = categoryService.getAllCategory().stream()
                .sorted((c1, c2) -> c2.getId().compareTo(c1.getId()))
                .limit(6)
                .toList();
        List<Product> products = productService.getAllActiveProduct("").stream()
                .sorted((p1, p2) -> p2.getId().compareTo(p1.getId()))
                .limit(8).toList();
        m.addAttribute("categories", categories);
        m.addAttribute("products", products);
        return "index";
    }

    @GetMapping("/signin")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String register() {
        return "register";
    }

    @GetMapping("/products")
    public String products(Model m, @RequestParam(value="category", defaultValue = "") String category) {
        List<Category> categories = categoryService.getAllActiveCategory();
        m.addAttribute("categories", categories);
        List<Product> products = productService.getAllActiveProduct(category);
        m.addAttribute("products", products);
        m.addAttribute("paramValue", category);
        return "product";
    }

    @GetMapping("/product/{id}")
    public String product(@PathVariable int id, Model m) {
        Product product = productService.getProduct(id);
        m.addAttribute("product", product);
        return "view_product";
    }

    //User
    @PostMapping("/saveUser")
    public String saveUser(@ModelAttribute User user, @RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) throws IOException {
        String imageName = file.isEmpty() ? "defaultImage.jpg" : file.getOriginalFilename();
        user.setImage(imageName);
        User saveUser = userService.saveUser(user);
        if(!ObjectUtils.isEmpty(saveUser)) {
            if(!file.isEmpty()) {
                File saveFile = new ClassPathResource("static/img").getFile();
                Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + "profile_img" + File.separator + file.getOriginalFilename());
                Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
            }
            redirectAttributes.addFlashAttribute("successMsg", "Congratulations! You have Successfully Registered");
        } else {
            redirectAttributes.addFlashAttribute("errorMsg", "Oops! Something is wrong");
        }
        return "redirect:/register";
    }

    //Forget Password section
    @GetMapping("/forgot_password")
    public String loadForgotPassword() {
        return "forgot_password";
    }

    @PostMapping("/forgot_password")
    public String processForgotPassword(@RequestParam String email, RedirectAttributes redirectAttributes, HttpServletRequest request) throws MessagingException, UnsupportedEncodingException {
        User user = userService.getUserByEmail(email);
        if(!ObjectUtils.isEmpty(user)) {
            String resetToken = UUID.randomUUID().toString();
            userService.updateUserResetToken(email, resetToken);
            //generate a link to send in email like-> http://localhost:8080/reset_password?token=something
            String url = CommonUtil.generateUrl(request)+"/reset_password?token="+resetToken;

            Boolean sendMail = commonUtil.sendMail(url, email);
            if(sendMail) {
                redirectAttributes.addFlashAttribute("successMsg", "A reset link has sent to your email. Please check your email to reset password.");
            } else {
                redirectAttributes.addFlashAttribute("errorMsg", "Internal Server Error!");
            }

        } else {
            redirectAttributes.addFlashAttribute("errorMsg", "Invalid Email!");
        }
        return "redirect:/forgot_password";
    }

    @GetMapping("/reset_password")
    public String loadResetPassword(@RequestParam String token, Model m) {
        User user = userService.getUserByToken(token);
        if(ObjectUtils.isEmpty((user))) {
            m.addAttribute("msg", "Your link is invalid or expired");
            return "message";
        }
        m.addAttribute("token", token);
        return "reset_password";
    }

    @PostMapping("/reset_password")
    public String resetPassword(@RequestParam String token, @RequestParam String password, Model m) {
        User user = userService.getUserByToken(token);
        if(ObjectUtils.isEmpty(user)) {
            m.addAttribute("msg", "Your link is invalid or expired");
            return "message";
        } else {
            user.setPassword(passwordEncoder.encode(password));
            user.setResetToken(null);
            userService.updateUser(user);
            m.addAttribute("msg", "Password Reset Successfully!");
            return "message";
        }
    }

    @GetMapping("/search")
    public String searchProduct(@RequestParam String ch, Model m) {
        List<Product> products = productService.searchProduct(ch);
        m.addAttribute("products", products);
        List<Category> categories = categoryService.getAllCategory();
        m.addAttribute("categories", categories);
        return "/product";
    }
}
