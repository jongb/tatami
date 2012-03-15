package fr.ippon.tatami.web.rest;

import fr.ippon.tatami.domain.User;
import fr.ippon.tatami.service.UserService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;

/**
 * REST controller for managing users.
 *
 * @author Julien Dubois
 */
@Controller
public class UserController {

    private final Log log = LogFactory.getLog(UserController.class);

    @Inject
    private UserService userService;

    @RequestMapping(value = "/rest/users/{login}",
            method = RequestMethod.GET,
            produces = "application/json")
    @ResponseBody
    public User getUser(@PathVariable("login") String login) {
        if (log.isDebugEnabled()) {
            log.debug("REST request to get Profile : " + login);
        }
        return userService.getUserProfileByLogin(login);
    }

    @RequestMapping(value = "/rest/users/{login}",
            method = RequestMethod.POST,
            consumes = "application/json")
    @ResponseBody
    public void updateUser(@PathVariable("login") String login, @RequestBody User user) {
        if (log.isDebugEnabled()) {
            log.debug("REST request to update user : " + login);
        }
        user.setLogin(login);
        userService.updateUser(user);
    }

    @RequestMapping(value = "/rest/users/{login}/addFriend",
            method = RequestMethod.POST,
            consumes = "application/json")
    public void addFriend(@PathVariable("login") String login, @RequestBody String friend) {
        if (log.isDebugEnabled()) {
            log.debug("REST request to add friend : " + friend);
        }
        User currentUser = userService.getCurrentUser();
        if (currentUser.getLogin().equals(login)) {
            userService.followUser(friend);
        } else {
            log.info("Cannot add a friend to another user");
        }
    }

    @RequestMapping(value = "/rest/users/{login}/removeFriend",
            method = RequestMethod.POST,
            consumes = "application/json",
            produces = "application/json")
    public void removeFriend(@PathVariable("login") String login, @RequestBody String friend) {
        if (log.isDebugEnabled()) {
            log.debug("REST request to remove friend : " + friend);
        }
        User currentUser = userService.getCurrentUser();
        if (currentUser.getLogin().equals(login)) {
            userService.forgetUser(friend);
        } else {
            log.info("Cannot remove a friend to another user");
        }
    }
}