package com.example.demo1.control;

import com.example.demo1.entity.User;
import com.example.demo1.entity.UserType;
import com.example.demo1.repository.UserRepository;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.List;

/**
 * Created by SeanCunniffe on 09/Apr/2022
 */

@Path("/user")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserControl {

    @Inject
    UserRepository userRepository;

    @GET
    public Response getUsers(@Context SecurityContext context){
        if(!context.isUserInRole(UserType.STAFF.name()))
            return abort("Unauthorized User", 403);
        List<User> users = userRepository.getUsers();
        return Response.ok().entity(users).build();
    }

    @GET
    @Path("search")
    public Response getUserByUsername(@QueryParam("username") String username, @Context SecurityContext sc){
        boolean isTheUser = sc.getUserPrincipal().getName().equals(username);
        if(!isTheUser && !sc.isUserInRole(UserType.STAFF.name())){
            return abort("Unauthorized User", 403);
        }
        User user = userRepository.getUserByUsername(username);
        return Response.ok().entity(user).build();
    }

    private static Response abort(String msg, int status){
        return Response.status(status).entity("{\"error\": \""+msg+"\"}").build();
    }
}
