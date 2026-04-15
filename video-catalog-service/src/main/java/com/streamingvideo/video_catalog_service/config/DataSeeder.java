package com.streamingvideo.video_catalog_service.config;

import com.streamingvideo.video_catalog_service.entity.Category;
import com.streamingvideo.video_catalog_service.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {
    private final CategoryRepository categoryRepository;

    @Override
    public void run(String... args) throws Exception {
        if (categoryRepository.count() == 0) {
            log.info("Bảng Category đang trống. Tiến hành seed category data...");
            List<Category> categories = List.of(
                    Category.builder().name("Giải Trí").slug("giai-tri").description("Video giải trí, hài hước, show truyền hình").build(),
                    Category.builder().name("Giáo Dục").slug("giao-duc").description("Bài giảng, khóa học, hướng dẫn").build(),
                    Category.builder().name("Âm Nhạc").slug("am-nhac").description("MV, cover, nhạc trực tiếp, podcast").build(),
                    Category.builder().name("Thể Thao").slug("the-thao").description("Highlights, phân tích trận đấu").build(),
                    Category.builder().name("Công Nghệ").slug("cong-nghe").description("Review sản phẩm, hướng dẫn lập trình").build(),
                    Category.builder().name("Du Lịch").slug("du-lich").description("Vlog du lịch, khám phá").build(),
                    Category.builder().name("Ẩm Thực").slug("am-thuc").description("Nấu ăn, review quán ăn").build(),
                    Category.builder().name("Gaming").slug("gaming").description("Stream game, walkthrough, esports").build()
            );

            categoryRepository.saveAll(categories);
            log.info("Đã tạo thành công {} danh mục mặc định.", categories.size());
        } else {
            log.info("Dữ liệu Category đã tồn tại, bỏ qua bước seed data.");
        }
    }
}
