ALTER TABLE herb_species
    ADD COLUMN cover_image_url VARCHAR(500) NULL COMMENT '药材封面图片地址' AFTER description;
