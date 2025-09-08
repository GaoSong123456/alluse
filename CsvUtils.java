package com.routdata.financial.common.utils;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.HeaderColumnNameMappingStrategy;
import com.pfpj.common.msg.UserInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.zip.Zip64Mode;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.lang.time.DateFormatUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
public class CsvUtils {
    private CsvUtils() {
    }

    /**
     * CSV文件列分隔符
     */
    private static final String CSV_COLUMN_SEPARATOR = ",";

    /**
     * CSV文件列分隔符
     */
    private static final String CSV_RN = "\r\n";

    // 读取字符编码
    private static final String UTF_8 = "UTF-8";

    /**
     * 解析csv文件，并且转成bean
     *
     * @param file
     * @param clazz
     * @param <T>
     * @return
     */
    public static <T> List<T> getCsvData(MultipartFile file, Class<T> clazz) {

        InputStreamReader in = null;
        try {
            // UTF-8为解析时用的编码格式，csv文件也需要定义为UTF-8编码格式，解析格式根据csv文件的编码格式而定，csv默认编码格式为GBK
            in = new InputStreamReader(file.getInputStream(), UTF_8);
        } catch (IOException e) {
            log.error("读取csv文件失败！ ---> " + e.getMessage());
        }
        HeaderColumnNameMappingStrategy<T> mappingStrategy = new HeaderColumnNameMappingStrategy<>();
        mappingStrategy.setType(clazz);
        CsvToBean<T> csvToBean = new CsvToBeanBuilder<T>(in)
                .withSeparator(',')
                .withIgnoreQuotations(true)
                .withMappingStrategy(mappingStrategy)
                .build();
        return csvToBean.parse();

    }

    /**
     * 解析csv文件并转成bean（方法二）
     *
     * @param file csv文件
     * @return 数组getCsvDataList
     */
    public static List<String[]> getCsvDataList(MultipartFile file) {

        List<String[]> list = new ArrayList<String[]>();
        int i = 0;
        try {
            CSVReader csvReader = new CSVReaderBuilder(
                    new BufferedReader(
                            new InputStreamReader(file.getInputStream(), "gbk"))).build();
            Iterator<String[]> iterator = csvReader.iterator();
            while (iterator.hasNext()) {
                String[] next = iterator.next();
                //去除第一行的表头，从第二行开始
                if (i >= 1) {
                    list.add(next);
                }
                i++;
            }
            return list;
        } catch (Exception e) {
            System.out.println("CSV文件读取异常");
            return list;
        }
    }

    /**
     * 从给定的 File 对象中解析 CSV 文件内容，返回一个 List<String[]>，其中每个元素代表一行数据。
     *
     * @param csvFile 包含 CSV 数据的 File 对象
     * @return 解析后的数据列表，每个元素是一个 String[]，表示一行数据
     * @throws IOException 如果读取或解析文件时发生异常
     */
    public static List<String[]> parseCsvFile(File csvFile) {
        try (CSVReader reader = new CSVReader(new InputStreamReader(new FileInputStream(csvFile), StandardCharsets.UTF_8))) {
            return reader.readAll();
        } catch (IOException e) {
            log.error(e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 解析csv文件并转成bean（方法二）
     * startRowNum 开始行数
     *
     * @param file csv文件
     * @return 数组
     */
    public static List<String[]> getCsvDataList(MultipartFile file, int startRowNum) {

        List<String[]> list = new ArrayList<String[]>();
        int i = 0;
        try {
            CSVReader csvReader = new CSVReaderBuilder(
                    new BufferedReader(
                            new InputStreamReader(file.getInputStream(), "gbk"))).build();
            Iterator<String[]> iterator = csvReader.iterator();
            while (iterator.hasNext()) {
                String[] next = iterator.next();
                //从第几行开始
                if (i >= startRowNum) {
                    if (!Arrays.stream(next).allMatch(String::isEmpty)) {
                        // 至少有一个元素不为空，可以进行添加操作
                        list.add(next);
                    }
                }
                i++;
            }
            return list;
        } catch (Exception e) {
            System.out.println("CSV文件读取异常");
            return list;
        }
    }

    /**
     * 解析csv文件并转成bean（方法三）
     *
     * @param file  csv文件
     * @param clazz 类
     * @param <T>   泛型
     * @return 泛型bean集合
     */
    public static <T> List<T> getCsvDataMethod3(MultipartFile file, Class<T> clazz) {
        CsvToBean<T> csvToBean = null;
        try (InputStreamReader in = new InputStreamReader(file.getInputStream(), UTF_8)) {
            HeaderColumnNameMappingStrategy<T> strategy = new HeaderColumnNameMappingStrategy<>();
            strategy.setType(clazz);
            csvToBean = new CsvToBeanBuilder<T>(in).withMappingStrategy(strategy).build();
        } catch (Exception e) {
            log.error("数据转化失败");
            return null;
        }
        return csvToBean.parse();
    }

    /**
     * @param dataList 集合数据
     * @param colNames 表头部数据
     * @param mapKey   查找的对应数据
     * @param os       返回结果
     *                 map： 查询参数
     *                 corpName:导出企业名称
     */
    public static boolean doExport(List<Map<String, Object>> dataList, String colNames, String mapKey, OutputStream os, Map<String, Object> map, Map<String, Object> corpName) {
        try {
            StringBuffer buf = new StringBuffer();

            String[] colNamesArr = null;
            String[] mapKeyArr = null;

            colNamesArr = colNames.split(",");
            mapKeyArr = mapKey.split(",");

            UserInfo info = SecurityUtils.getUserInfo();
            if (!corpName.isEmpty()) {
                for (Map.Entry<String, Object> entry : corpName.entrySet()) {
                    buf.append(entry.getKey()).append(CSV_COLUMN_SEPARATOR).append(entry.getValue()).append(CSV_RN);
                }
            }
            buf.append("操作人:")
                    .append(CSV_COLUMN_SEPARATOR)
                    .append(info.getUserName())
                    .append(CSV_COLUMN_SEPARATOR)
                    .append("操作时间")
                    .append(CSV_COLUMN_SEPARATOR)
                    .append(DateFormatUtils.format(new Date(), "yyyy-MM-dd hh:mm:ss")).append(CSV_RN);
            if (!map.isEmpty()) {
                buf.append("查询条件:").append(CSV_COLUMN_SEPARATOR);
                for (Map.Entry<String, Object> entry : map.entrySet()) {
                    System.out.println(entry.getKey() + "--->" + entry.getValue());
                    buf.append(entry.getKey()).append(CSV_COLUMN_SEPARATOR).append(entry.getValue())
                            .append(CSV_COLUMN_SEPARATOR).append("").append(CSV_COLUMN_SEPARATOR);
                }
                buf.append(CSV_RN);
            }

            // 完成数据csv文件的封装
            // 输出列头
            for (String aColNamesArr : colNamesArr) {
                buf.append(aColNamesArr).append(CSV_COLUMN_SEPARATOR);
            }
            buf.append(CSV_RN);

            if (null != dataList) { // 输出数据
                for (Map<String, Object> aDataList : dataList) {
                    for (String aMapKeyArr : mapKeyArr) {
                        buf.append(aDataList.get(aMapKeyArr)).append(CSV_COLUMN_SEPARATOR);
                    }
                    buf.append(CSV_RN);
                }
            }
            // 写出响应
            os.write(buf.toString().getBytes("utf8"));
            os.flush();
            return true;
        } catch (Exception e) {
            log.error("doExport错误...", e);
        }
        return false;
    }

    /**
     * setHeader
     */
    public static void responseSetProperties(String fileName, HttpServletResponse response) throws UnsupportedEncodingException {
        // 设置文件后缀
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        String fn = fileName + sdf.format(new Date()) + ".csv";

        // 设置响应
        response.setContentType("application/ms-txt.numberformat:@");
        response.setCharacterEncoding(UTF_8);
        response.setHeader("Pragma", "public");
        response.setHeader("Cache-Control", "max-age=30");
        response.setHeader("Content-Disposition", "attachment; filename=" + URLEncoder.encode(fn, UTF_8));
    }


//    public static File createCSVFile(List exportData, LinkedHashMap map,
//                                     String outPutPath, String fileName) {
//        File csvFile = null;
//        BufferedWriter csvFileOutputStream = null;
//        try {
//            File file = new File(outPutPath);
//            if (!file.exists()) {
//                file.mkdir();
//            }
//            // 定义文件名格式并创建
//            csvFile = File.createTempFile(fileName, ".csv",
//                    new File(outPutPath));
//            logger.debug("csvFile：" + csvFile);
//            // UTF-8使正确读取分隔符","
//            csvFileOutputStream = new BufferedWriter(new OutputStreamWriter(
//                    new FileOutputStream(csvFile), "GBK"), 1024);
//            logger.debug("csvFileOutputStream：" + csvFileOutputStream);
//            // 写入文件头部
//            for (Iterator propertyIterator = map.entrySet().iterator(); propertyIterator
//                    .hasNext();) {
//                java.util.Map.Entry propertyEntry = (java.util.Map.Entry) propertyIterator
//                        .next();
//                csvFileOutputStream
//                        .write("" + (String) propertyEntry.getValue() != null ? (String) propertyEntry
//                                .getValue() : "" + "");
//                if (propertyIterator.hasNext()) {
//                    csvFileOutputStream.write(",");
//                }
//            }
//            csvFileOutputStream.newLine();
//            // 写入文件内容
//            for (Iterator iterator = exportData.iterator(); iterator.hasNext();) {
//                Object row = (Object) iterator.next();
//                for (Iterator propertyIterator = map.entrySet().iterator(); propertyIterator
//                        .hasNext();) {
//                    java.util.Map.Entry propertyEntry = (java.util.Map.Entry) propertyIterator
//                            .next();
//                    csvFileOutputStream.write((String) BeanUtils.getProperty(
//                            row, (String) propertyEntry.getKey()));
//                    if (propertyIterator.hasNext()) {
//                        csvFileOutputStream.write(",");
//                    }
//                }
//                if (iterator.hasNext()) {
//                    csvFileOutputStream.newLine();
//                }
//            }
//            csvFileOutputStream.flush();
//        } catch (Exception e) {
//            e.printStackTrace();
//        } finally {
//            try {
//                csvFileOutputStream.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//        return csvFile;
//    }

    /**
     * 删除该目录filePath下的所有文件
     *
     * @param filePath 文件目录路径
     */
    public static void deleteFiles(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            File[] files = file.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isFile()) {
                    if (!files[i].delete()) {
                        log.error("删除文件 {} 失败", files[i]);
                    }
                }
            }
        }
    }

    /**
     * 将磁盘的多个文件打包成压缩包并输出流下载
     *
     * @param pathList
     * @param request
     * @param response
     */
    public static void zipDirFileToFile(List<Map<String, String>> pathList, HttpServletRequest request, HttpServletResponse response) {
        try {
            // 设置response参数并且获取ServletOutputStream
            ZipArchiveOutputStream zous = getServletOutputStream(response);

            for (Map<String, String> map : pathList) {
                String fileName = map.get("name");
                File file = new File(map.get("path"));
                InputStream inputStream = Files.newInputStream(file.toPath());
                setByteArrayOutputStream(fileName, inputStream, zous);
            }
            zous.close();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private static ZipArchiveOutputStream getServletOutputStream(HttpServletResponse response) throws Exception {

        String outputFileName = "文件" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + ".zip";
        response.reset();
        response.setHeader("Content-Type", "application/octet-stream");
        response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(outputFileName, UTF_8));
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Cache-Control", "no-cache");
        response.addHeader("Access-Control-Allow-Origin", "*");//允许所有来源访同
        response.addHeader("Access-Control-Allow-Method", "POST,GET");
        ServletOutputStream out = response.getOutputStream();

        ZipArchiveOutputStream zous = new ZipArchiveOutputStream(out);
        zous.setUseZip64(Zip64Mode.AsNeeded);
        return zous;
    }

    private static void setByteArrayOutputStream(String fileName, InputStream inputStream, ZipArchiveOutputStream zous) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            baos.write(buffer, 0, len);
        }
        baos.flush();
        byte[] bytes = baos.toByteArray();

        //设置文件名
        ArchiveEntry entry = new ZipArchiveEntry(fileName);
        zous.putArchiveEntry(entry);
        zous.write(bytes);
        zous.closeArchiveEntry();
        baos.close();
    }

    public static String convertFormat(String data) throws Exception {
        if (data.matches("[-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?")) {
            BigDecimal db = new BigDecimal(data);
            return db.toPlainString();
        }
        return data;
    }


}
