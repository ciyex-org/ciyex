package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ListOptionDto;
import com.qiaben.ciyex.service.ListOptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;

@RestController
@RequestMapping("/api/list-options")
public class ListOptionController {

    private final ListOptionService service;

    @Autowired
    public ListOptionController(ListOptionService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody ListOptionDto dto) {
        List<String> missing = new ArrayList<>();
        if (dto.getListId() == null || dto.getListId().trim().isEmpty()) missing.add("listId");
        if (dto.getTitle() == null || dto.getTitle().trim().isEmpty()) missing.add("title");
        if (dto.getSeq() == null) missing.add("seq");
        if (dto.getActivity() == null) missing.add("activity");

        if (!missing.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Missing required fields",
                    "missing", missing
            ));
        }

        ListOptionDto created = service.create(dto);
        return ResponseEntity.ok(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable String id, @RequestBody ListOptionDto dto) {
        List<String> missing = new ArrayList<>();
        if (dto.getListId() == null || dto.getListId().trim().isEmpty()) missing.add("listId");
        if (dto.getTitle() == null || dto.getTitle().trim().isEmpty()) missing.add("title");
        if (dto.getSeq() == null) missing.add("seq");
        if (dto.getActivity() == null) missing.add("activity");
        if (!missing.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Missing required fields",
                    "missing", missing
            ));
        }
        ListOptionDto updated = service.update(id, dto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ListOptionDto> get(@PathVariable String id) {
        ListOptionDto dto = service.get(id);
        return ResponseEntity.ok(dto);  // Response will be in JSON by default
    }

    @GetMapping
    public ResponseEntity<List<ListOptionDto>> getAll() {
        List<ListOptionDto> list = service.getAll();
        return ResponseEntity.ok(list);
    }



        // Endpoint to get a list option by list_id
        @GetMapping("/list/{list_id}")
        public ResponseEntity<List<ListOptionDto>> getListOptionsByListId(@PathVariable String list_id) {
            // Call the service to get list options based on the list_id
            List<ListOptionDto> listOptions = service.getListOptionsByListId(list_id);
            return ResponseEntity.ok(listOptions);  // Response will be in JSON by default
        }

        @DeleteMapping("/list/{list_id}")
        public ResponseEntity<Void> deleteByListId(@PathVariable String list_id) {
           service.deleteByListId(list_id);
           return ResponseEntity.noContent().build();  // Return 204 No Content
        }

}