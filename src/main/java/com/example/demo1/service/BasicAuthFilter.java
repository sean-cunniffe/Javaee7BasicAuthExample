package com.example.demo1.service;

import com.example.demo1.entity.User;
import com.example.demo1.entity.UserType;
import com.example.demo1.repository.UserRepository;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by SeanCunniffe on 09/Apr/2022
 */

@Provider
public class BasicAuthFilter implements ContainerRequestFilter {

    @Inject
    UserRepository userRepository;

    Map<String, List<UserType>> allowedEndpoints;

    /**
     * <p>Create a map that pairs user types to the url they're allowed to access
     * you could go one step further and change this to only allow certain methods
     * (customer can only GET, and staff can GET, PUT, DELETE)</p><br>
     * <p>
     * you could go another step further and use regex to allow only particular url endpoints
     * (customer can access /users/{id} but not /users)
     * </p>
     */
    @PostConstruct
    public void postConstruct(){
        allowedEndpoints = new HashMap<>();
        allowedEndpoints.put("/user/", Arrays.asList(UserType.STAFF, UserType.CUSTOMER));
        allowedEndpoints.put("/order/", Arrays.asList(UserType.STAFF));
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {

        // check the path requested if it has to be secure
        String path = requestContext.getUriInfo().getPath();
        // can't use allowedEndpoints.get(path) because we want to return paths
        // like /user/search as well if the url contains /user/
        // More complex ways of doing this with regex but this is a simple example
        List<UserType> userTypes = allowedEndpoints
                .entrySet()
                .stream()
                .filter(stringListEntry -> path.contains(stringListEntry.getKey()))
                .flatMap(stringListEntry -> stringListEntry.getValue().stream()).collect(Collectors.toList());
        // if no UserTypes are returned we can assume the endpoint isn't secured eg. /auth for creating token when using JWT
        if(userTypes.isEmpty())
            return;
        try {
            List<String> authorization = requestContext.getHeaders().get("authorization");
            // Basic {base64}
            String base64Header = authorization.get(0);
            // base64
            String parsePrefix = base64Header.substring(6);
            // username:password
            String decoded = new String(Base64.getDecoder().decode(parsePrefix.getBytes()));
            // [username, password]
            String[] joinedCredentials = decoded.split(":");
            String username = joinedCredentials[0];
            String password = joinedCredentials[1];

            User user = userRepository.getUserByUsernameAndPassword(username, password);
            if (user == null) {
                abort(requestContext, "Unauthorized access", 401);
                return;
            }

            if(!userTypes.contains(user.getUserType())){
                abort(requestContext, "Restricted Access", 403);
            }
            // if it gets to here it has the right privileges
            // create security context we can pass to the methods if we need it
            SecurityContext securityContext = createSecurityContext(user);
            requestContext.setSecurityContext(securityContext);

        }catch (NullPointerException e){
            e.printStackTrace();
            // no header was found or the
            abort(requestContext, "Unauthorized access", 401);
        } catch (ArrayIndexOutOfBoundsException e){
            e.printStackTrace();
            // header formatted incorrectly
            abort(requestContext, "Authorization header required", 401);
        }
    }

    private SecurityContext createSecurityContext(User user) {
        return new SecurityContext() {
            @Override
            public Principal getUserPrincipal() {
                return user::getUsername;
            }

            @Override
            public boolean isUserInRole(String role) {
                return role.equals(user.getUserType().name());
            }

            @Override
            public boolean isSecure() {
                return true;
            }

            @Override
            public String getAuthenticationScheme() {
                return "BASIC";
            }
        };
    }

    private void abort(ContainerRequestContext requestContext, String msg, int status) {
        String jsonMsg = "{\"error\": \"" + msg + "\"}";
        requestContext.abortWith(Response.status(status).entity(jsonMsg).build());
    }
}

