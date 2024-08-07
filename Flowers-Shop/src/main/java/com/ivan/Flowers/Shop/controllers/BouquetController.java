package com.ivan.Flowers.Shop.controllers;

import com.ivan.Flowers.Shop.models.dtos.BouquetDTO;
import com.ivan.Flowers.Shop.services.BouquetService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;

@Controller
public class BouquetController {

    private final BouquetService bouquetService;

    public BouquetController(BouquetService bouquetService) {
        this.bouquetService = bouquetService;
    }

    @GetMapping("/bouquet/add")
    public ModelAndView addBouquet(BouquetDTO bouquetDTO) {

        return new ModelAndView("bouquet-add");

    }

    @PostMapping("/bouquet/add")
    public ModelAndView addBouquet(@Valid BouquetDTO bouquetDTO, BindingResult bindingResult) throws IOException {

        if (bindingResult.hasErrors()) {
            return new ModelAndView("bouquet-add");
        }

        boolean isAdded = bouquetService.addBouquet(bouquetDTO);

        if (!isAdded) {
            return new ModelAndView("bouquet-add");
        }

        return new ModelAndView("redirect:/home");
    }

    //TODO: implement delete mapping
    @PostMapping("/bouquet/remove/{itemNumber}")
    public ModelAndView remove(@PathVariable("itemNumber") int itemNumber) throws IOException {

        bouquetService.removeBouquet(itemNumber);

        return new ModelAndView("redirect:/home");
    }

}
