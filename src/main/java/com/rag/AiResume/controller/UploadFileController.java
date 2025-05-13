package com.rag.AiResume.controller;

import com.rag.AiResume.service.UploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class UploadFileController {

  private UploadService uploadService;

  @Autowired
  public UploadFileController(UploadService uploadService) {
    this.uploadService = uploadService;
  }

  @PostMapping(value = "upload-file")
  public String HandleUploadFile(
    @RequestPart("File") MultipartFile file,
    @RequestPart("userId") String userId
  ) {
    String target = "data";
    System.out.println("-userId-" + userId);
    return this.uploadService.handleSaveUpLoadFile(file, target);
  }
}
