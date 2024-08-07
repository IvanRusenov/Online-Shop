package com.ivan.Flowers.Shop.services.impls;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.ivan.Flowers.Shop.services.CloudinaryService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

@Service
public class CloudinaryServiceImpl implements CloudinaryService {

    private final Cloudinary cloudinary;

    public CloudinaryServiceImpl(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    @Override
    public String uploadImage(MultipartFile multipartFile) throws IOException {

        File file = File
                .createTempFile("temp-file", multipartFile.getOriginalFilename());

        multipartFile.transferTo(file);

        return this.cloudinary.uploader()
                .upload(file, new HashMap())
                .get("url").toString();

    }

    @Override
    public boolean deleteImage(String imageUrl) throws IOException {

        String[] split = imageUrl.split("/");
        String s = split[split.length - 1];
        String publicId = s.split("\\.")[0];

        cloudinary.uploader().destroy(publicId,
                ObjectUtils.asMap("resource_type","image"));

        return true;
    }

}
