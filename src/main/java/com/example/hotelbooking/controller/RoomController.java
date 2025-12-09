package com.example.hotelbooking.controller;

import com.example.hotelbooking.model.Room;
import com.example.hotelbooking.repository.RoomRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/rooms")
public class RoomController {
    
    private final RoomRepository roomRepository;
    
    public RoomController(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }
    
    @GetMapping
    public String listRooms(Model model) {
        List<Room> rooms = roomRepository.findByAvailableTrue();
        model.addAttribute("rooms", rooms);
        return "rooms";
    }
    
    @GetMapping("/available")
    public String availableRooms(Model model) {
        List<Room> rooms = roomRepository.findByAvailableTrue();
        model.addAttribute("rooms", rooms);
        return "rooms";
    }
}