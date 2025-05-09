# Split comic panel

### Introduction

Split comic images.

Các bước:

- Sử dụng chương trình Python (OpenCV) hay PHP để tự động tách thành các frame.
- Sử dụng trang web (JS) để [review](http://192.168.1.48/posts/project - comic split/review.html) lại.
- Cắt các ảnh bằng PHP.

[Viewer](http://192.168.1.48/posts/project - comic split/viewer.html)

Chia ảnh ra thành các panel (hàng, ô) nhỏ để dễ đọc trên các thiết bị như điện thoại di động.

Xử lý ảnh thật là hay, có nhiều ý tưởng hữu ích: nhận dạng biển số, nhận dạng cháy, đọc số công tơ, phát hiện trộm,...

[thigiacmaytinh.com](https://thigiacmaytinh.com/)

Để làm những cái đó, chúng ta nên sử dụng OpenCV.

Việc tách các khung truyện tranh thì sử dụng Pillow của Python thôi.

Tách ảnh, đóng gói file cbz, đọc trên iPad.

### Thuật toán

Cấu trúc của 1 trang truyện là:

```
  +---Page-------------------------------------+
  |                                            | <- Gutter
  | +-----------+ +-----------+ +-----------+  |-----
  | |           | |           | |           |  |
  | |   Frame   | |   Frame   | |   Frame   |  | Row
  | |           | |           | |           |  |
  | +-----------+ +-----------+ +-----------+  |-----
  |                                            | <- Gutter
  | +-----------+ +-----------+ +-----------+  |-----
  | |           | |           | |           |  |
  | |   Frame   | |   Frame   | |   Frame   |  | Row
  | |           | |           | |           |  |
  | +-----------+ +-----------+ +-----------+  |-----
  |                                            | <- Gutter
  +--------------------------------------------+
                <->
              Gutter
```

Gutter là các khoảng không gian trống chia các panel. Gutter trái, phải, trên, dưới có thể không tồn tại.

Chúng ta có thể kiểm tra 1 đường thẳng (ngang hoặc dọc) có phải là gutter hay không nếu nó chỉ có một màu.

Các bước:

- Có thể chuyển ảnh về dạng mono hoặc tăng độ tương phản cho đỡ nhiễu, dễ tách (`mono_creator.py`).
- Duyệt theo chiều dọc, từ trên xuống dưới, tách thành các hàng.
- Với mỗi hàng, duyệt theo chiều ngang, từ trái sang phải, tách thành các ô.
- Ghi ra file ảnh hoặc JSON (`file_writer.py`).

Với các hàng (rows), các ô (frames), chúng ta lưu 4 thông tin (left, top, right, bottom) theo pixel (hay theo phần trăm).

Thuật toán:

- Bắt đầu từ một `startRow`, chúng ta di chuyển xuống dưới theo chiều dọc khi mà cả dòng vẫn còn là gutter. Khi không còn là gutter nữa thì sẽ là bắt đầu của một dòng.
- Tiếp theo vẫn di chuyển xuống dưới tiếp, nhưng mỗi lần di chuyển sẽ là `fheight `pixel (không phải là từng pixel một cho nhanh). `fheight `là chiều cao tối thiểu của một ô. Di chuyển cho đến khi gặp một gutter nữa hoặc đã đến cuối trang. Đó sẽ là kết thúc một dòng.

Một khi chúng ta đã có các dòng (rows), chúng ta lặp lại các thao tác trên cho từng dòng. Bây giờ chúng ta sẽ di chuyển theo chiều ngang từ trái qua phải.

###  Hạn chế

Chương trình có thể không split thành công các ô từ một trang mà bị xoay.

Ở một số trang, một số ảnh có thể "overflow" vào vùng gutter. Trong trường hợp đó, chương trình có thể không tách được 2 ô.

Nếu gutter không "clean" (ví dụ ảnh scan chất lượng thấp, chứa các pixel đen ngẫu nhiên) thì cũng có thể không tách được. Ảnh "clean" thường là ảnh mà gutter đều màu trắng.

Một trang truyện thường bao gồm nhiều khung (frame) được ngăn cách bằng các vùng trắng ngang/dọc gọi là gutter. Trang tiêu đề có thể có thêm heading ở đầu.

### Tham khảo

[GitHub - njean42/kumiko: Kumiko, the Comics Cutter](https://github.com/njean42/kumiko)

[Ajira-FR/comics-splitter: Comics Splitter is a Python script that cut comic strip or manga page in panels. This allow the reader to have a better reading experience on small devices like ereader, smartphone or tablet](https://github.com/Ajira-FR/comics-splitter/blob/master/comics_splitter.py)

[OpenCV: Contour Features](https://docs.opencv.org/3.4/dc/dcf/tutorial_js_contour_features.html)

[OpenCV Python Tutorial - GeeksforGeeks](https://www.google.com/amp/s/www.geeksforgeeks.org/opencv-python-tutorial/amp/)

[Tìm vị trí của text trong ảnh - THỊ GIÁC MÁY TÍNH](https://thigiacmaytinh.com/tim-vi-tri-cua-text-trong-anh/)

[prototype floodfill gap closing demo - YouTube](https://www.youtube.com/watch?v=2LiAM7KVTxA)

https://github.com/mypaint/mypaint/blob/master/lib/fill/gap_closing_fill.cpp

https://www1.lunapic.com/editor/?action=paint-bucket

https://docs.opencv.org/3.4/db/df6/tutorial_erosion_dilatation.html

https://krita-artists.org/uploads/default/original/3X/7/f/7fdf1d0b0bd8ddd5125f54e6c904f7df168f68b1.png

[Comic book panel segmentation • Max Halford](https://maxhalford.github.io/blog/comic-book-panel-segmentation/)



Mô tả:

comic, image, frame, panel to panel, php gd, opencv, python, contour, resizable, resize polygon, drag position

