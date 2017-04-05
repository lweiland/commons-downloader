# commons-downloader

Added Option --folder and multi threaded approach.
```java -jar commons-downloader.jar --folder txt/ --destination img```

txt/ contains .txt files with one image name per line, newline indicated with ","
e.g., 0.txt, looks like:
The_Crusades_(1935_film)_video_boxart.jpg,8000 

Silver_Chalice_poster.jpg,8000

The_Daughter_of_Time_-_Josephine_Tey.JPG,8000

White_goddess.JPG,8000

Omar_khayyam_tape_cover.JPG,8000

The_Gospel_According_to_Peanuts_(book_cover).jpg,8000

Rubaiyat_cover.JPG,8000

Passover_plot.JPG,8000


in folder img/ for each .txt file a separate folder will be created with the name of the txt
e.g., 0/

the images are downloaded in original size


~ takes 30mins for 50,000 images


**TODO**
- add @Option --poolsize for defining the number of threads in ThreadPool (I went back from Runtime.getRuntime().availableProcessors() to a fixed size of 15Threads)
- additional testing
