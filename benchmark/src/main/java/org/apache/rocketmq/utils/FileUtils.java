package org.apache.rocketmq.utils;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class FileUtils {
    private static final Logger logger = LoggerFactory.getLogger(FileUtils.class);

//    public static File createFile(String fileName) {
//        try {
//            File file = new File("exampleWrite.json");
//            if (file.createNewFile()) {
//                logger.info("文件创建成功！");
//            } else {
//                logger.info("出错了，该文件已经存在");
//            }
//        } catch (IOException ioe) {
//            ioe.printStackTrace();
//        }
//    }


    public static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        return dir.delete();
    }


    public static String getMD5(File dest) throws IOException {
        return DigestUtils.md5Hex(new FileInputStream(dest));
    }
}
