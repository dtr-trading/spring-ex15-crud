package com.dtr.oas.controller;

import java.util.List;
import java.util.Locale;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.dtr.oas.exception.DuplicateUserException;
import com.dtr.oas.exception.RoleNotFoundException;
import com.dtr.oas.exception.UserNotFoundException;
import com.dtr.oas.model.Role;
import com.dtr.oas.model.User;
import com.dtr.oas.service.RoleService;
import com.dtr.oas.service.UserService;

@Controller
@RequestMapping(value = "/user")
@PreAuthorize("denyAll")
public class UserController {
    static Logger logger = LoggerFactory.getLogger(UserController.class);
    static String businessObject = "user"; //used in RedirectAttributes messages 

    @Autowired
    private RoleService roleService;

    @Autowired
    private UserService userService;

    @Autowired
    private MessageSource messageSource;

    @ModelAttribute("allRoles")
    @PreAuthorize("hasAnyRole('CTRL_USER_LIST_GET','CTRL_USER_EDIT_GET')")
    public List<Role> getAllRoles() {
        return roleService.getRoles();
    }

    @ModelAttribute("enabledOptions")
    @PreAuthorize("hasAnyRole('CTRL_USER_LIST_GET','CTRL_USER_EDIT_GET')")
    public boolean[] getEnabledOptions() {
        boolean[] array = new boolean[2];
        array[0] = true;
        array[1] = false;
        return array;
    }

    @RequestMapping(value = {"/", "/list"}, method = RequestMethod.GET)
    @PreAuthorize("hasRole('CTRL_USER_LIST_GET')")
    public String listUsers(Model model) {
        logger.info("IN: User/list-GET");

        List<User> users = userService.getUsers();
        model.addAttribute("users", users);

        // if there was an error in /add, we do not want to overwrite
        // the existing user object containing the errors.
        if (!model.containsAttribute("userDTO")) {
            logger.info("Adding UserDTO object to model");
            UserDTO userDTO = new UserDTO();
            model.addAttribute("userDTO", userDTO);
        }
        return "user-list";
    }

    @RequestMapping(value = "/add", method = RequestMethod.POST)
    @PreAuthorize("hasRole('CTRL_USER_ADD_POST')")
    public String addUser(@Valid @ModelAttribute UserDTO userDTO,
            BindingResult result, RedirectAttributes redirectAttrs) {
        
        logger.info("IN: User/add-POST");

        if (result.hasErrors()) {
            logger.info("UserDTO add error: " + result.toString());
            redirectAttrs.addFlashAttribute("org.springframework.validation.BindingResult.userDTO", result);
            redirectAttrs.addFlashAttribute("userDTO", userDTO);
            return "redirect:/user/list";
        } else {
            User user = new User();

            try {
                user = getUser(userDTO);
                userService.addUser(user);
                String message = messageSource.getMessage("ctrl.message.success.add", new Object[] {businessObject, user.getUsername()}, Locale.US);
                redirectAttrs.addFlashAttribute("message", message);
                return "redirect:/user/list";
            } catch (DuplicateUserException e) {
                String message = messageSource.getMessage("ctrl.message.error.duplicate", new Object[] {businessObject, userDTO.getUsername()}, Locale.US);
                redirectAttrs.addFlashAttribute("error", message);
                return "redirect:/user/list";
            } catch (RoleNotFoundException rnf) {
                String message = messageSource.getMessage("ctrl.message.error.notfound", new Object[] {"role", userDTO.getRoleId()}, Locale.US);
                result.rejectValue("roleId", "0", message);
                redirectAttrs.addFlashAttribute("org.springframework.validation.BindingResult.userDTO", result);
                redirectAttrs.addFlashAttribute("userDTO", userDTO);
                return "redirect:/user/list";
            }
        }
    }

    @RequestMapping(value = "/edit", method = RequestMethod.GET)
    @PreAuthorize("hasRole('CTRL_USER_EDIT_GET')")
    public String editUserPage(@RequestParam(value = "id", required = true)
            Integer id, Model model, RedirectAttributes redirectAttrs) {

        logger.info("IN: User/edit-GET:  ID to query = " + id);

        try {
            if (!model.containsAttribute("userDTO")) {
                logger.info("Adding userDTO object to model");
                User user = userService.getUser(id);
                UserDTO userDTO = getUserDTO(user);
                logger.info("User/edit-GET:  " + userDTO.toString());
                model.addAttribute("userDTO", userDTO);
            }
            return "user-edit";
        } catch (UserNotFoundException e) {
            String message = messageSource.getMessage("ctrl.message.error.notfound", new Object[] {"user id", id}, Locale.US);
            model.addAttribute("error", message);
            return "redirect:/user/list";
        }
    }

    @RequestMapping(value = "/edit", method = RequestMethod.POST)
    @PreAuthorize("hasRole('CTRL_USER_EDIT_POST')")
    public String editUser(@Valid @ModelAttribute UserDTO userDTO,
            BindingResult result, RedirectAttributes redirectAttrs,
            @RequestParam(value = "action", required = true) String action) {

        logger.info("IN: User/edit-POST: " + action);

        if (action.equals(messageSource.getMessage("button.action.cancel", null, Locale.US))) {
            String message = messageSource.getMessage("ctrl.message.success.cancel", new Object[] {"Edit", businessObject, userDTO.getUsername()}, Locale.US);
            redirectAttrs.addFlashAttribute("message", message);
        } else if (result.hasErrors()) {
            logger.info("User-edit error: " + result.toString());
            redirectAttrs.addFlashAttribute("org.springframework.validation.BindingResult.userDTO", result);
            redirectAttrs.addFlashAttribute("userDTO", userDTO);
            return "redirect:/user/edit?id=" + userDTO.getId();
        } else if (action.equals(messageSource.getMessage("button.action.save",  null, Locale.US))) {
            logger.info("User/edit-POST:  " + userDTO.toString());
            try {
                User user = getUser(userDTO);
                userService.updateUser(user);
                String message = messageSource.getMessage("ctrl.message.success.update", new Object[] {businessObject, userDTO.getUsername()}, Locale.US);
                redirectAttrs.addFlashAttribute("message", message);
            } catch (DuplicateUserException unf) {
                String message = messageSource.getMessage("ctrl.message.error.duplicate", new Object[] {businessObject, userDTO.getUsername()}, Locale.US);
                redirectAttrs.addFlashAttribute("error", message);
                return "redirect:/user/list";
            } catch (UserNotFoundException unf) {
                String message = messageSource.getMessage("ctrl.message.error.notfound", new Object[] {businessObject, userDTO.getUsername()}, Locale.US);
                redirectAttrs.addFlashAttribute("error", message);
                return "redirect:/user/list";
            } catch (RoleNotFoundException rnf) {
                String message = messageSource.getMessage("ctrl.message.error.notfound", new Object[] {"role", userDTO.getRoleId()}, Locale.US);
                redirectAttrs.addFlashAttribute("error", message);
                return "redirect:/user/list";
            }
        }
        return "redirect:/user/list";
    }

    @RequestMapping(value = "/delete", method = RequestMethod.GET)
    @PreAuthorize("hasRole('CTRL_USER_DELETE_GET')")
    public String deleteUser(
            @RequestParam(value = "id", required = true) Integer id,
            @RequestParam(value = "phase", required = true) String phase,
            Model model, RedirectAttributes redirectAttrs) {

        User user;
        try {
            user = userService.getUser(id);
        } catch (UserNotFoundException e) {
            String message = "User number [" + id + "] does not exist in the system";
            redirectAttrs.addFlashAttribute("error", message);
            return "redirect:/user/list";
        }

        logger.info("IN: User/delete-GET | id = " + id + " | phase = " + phase + " | " + user.toString());

        if (phase.equals(messageSource.getMessage("button.action.cancel", null, Locale.US))) {
            String message = messageSource.getMessage("ctrl.message.success.cancel", new Object[] {"Delete", businessObject, user.getUsername()}, Locale.US);
            redirectAttrs.addFlashAttribute("message", message);
            return "redirect:/user/list";
        } else if (phase.equals(messageSource.getMessage("button.action.stage", null, Locale.US))) {
            logger.info("     adding user : " + user.toString());
            model.addAttribute("user", user);
            return "user-delete";
        } else if (phase.equals(messageSource.getMessage("button.action.delete", null, Locale.US))) {
            try {
                userService.deleteUser(user.getId());
                String message = messageSource.getMessage("ctrl.message.success.delete", new Object[] {businessObject, user.getUsername()}, Locale.US);
                redirectAttrs.addFlashAttribute("message", message);
                return "redirect:/user/list";
            } catch (UserNotFoundException e) {
                String message = messageSource.getMessage("ctrl.message.error.notfound", new Object[] {"user id", id}, Locale.US);
               redirectAttrs.addFlashAttribute("error", message);
                return "redirect:/user/list";
           }
        }

        return "redirect:/user/list";
    }

    @PreAuthorize("hasAnyRole('CTRL_USER_EDIT_GET','CTRL_USER_DELETE_GET')")
    public UserDTO getUserDTO(User user) {
        UserDTO userDTO = new UserDTO();
        userDTO.setId(user.getId());
        userDTO.setUsername(user.getUsername());
        userDTO.setPassword(user.getPassword());
        userDTO.setEnabled(user.getEnabled());
        userDTO.setRoleId(user.getRole().getId());
        return userDTO;
    }

    @PreAuthorize("hasAnyRole('CTRL_USER_ADD_POST','CTRL_USER_EDIT_POST')")
    public User getUser(UserDTO userDTO) throws RoleNotFoundException {
        User user = new User();
        user.setId(userDTO.getId());

        Role role = roleService.getRole(userDTO.getRoleId());
        user.setRole(role);

        user.setUsername(userDTO.getUsername());
        user.setPassword(userDTO.getPassword());
        user.setEnabled(userDTO.getEnabled());
        return user;
    }
}
