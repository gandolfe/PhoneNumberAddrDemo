package com.example.numberaddrdemo;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabaseCorruptException;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Scanner;


public class AddressQueryUtil {


    private static final String TAG = "AddressQueryUtil";

    private static Map<String, String> specialnum;

    private static AddressQueryUtil instance;

    private static final String[] PHONE_NUMBER_TYPE = {null, "移动", "联通", "电信", "电信虚拟运营商", "联通虚拟运营商", "移动虚拟运营商"};
    private static final int INDEX_SEGMENT_LENGTH = 9;
    private static final int DATA_FILE_LENGTH_HINT = 3747505;

    private static byte[] dataByteArray;
    private static int indexAreaOffset;
    private static int phoneRecordCount;

    private ByteBuffer byteBuffer;

    static {
        specialnum = new ArrayMap<>();
        specialnum.put("110", "报警电话");
        specialnum.put("120", "急救电话");
        specialnum.put("122", "交通报警");
        specialnum.put("999", "红十字会");
        specialnum.put("112", "紧急呼叫");
        specialnum.put("12345", "政府公益");
        specialnum.put("1008611", "移动客服");
        specialnum.put("10086", "移动客服");
        specialnum.put("10000", "电信客服");
        specialnum.put("10010", "联通客服");
        specialnum.put("10011", "联通客服");
    }

    private AddressQueryUtil() {
        initData();
        byteBuffer = ByteBuffer.wrap(dataByteArray);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
    }

    public static AddressQueryUtil getInstance() {
        if (instance == null) {
            instance = new AddressQueryUtil();
        }
        return instance;
    }

    private synchronized void initData() {
        if (dataByteArray == null) {
            ByteArrayOutputStream byteData = new ByteArrayOutputStream(DATA_FILE_LENGTH_HINT);
            byte[] buffer = new byte[1024];

            int readBytesLength;
            try {
                InputStream inputStream = BtApplication.getApplication().getAssets().open("phone.dat");
                while ((readBytesLength = inputStream.read(buffer)) != -1) {
                    byteData.write(buffer, 0, readBytesLength);
                }
            } catch (Exception e) {
                System.err.println("Can't find phone.dat in classpath: ");
                e.printStackTrace();
                throw new RuntimeException(e);
            }

            dataByteArray = byteData.toByteArray();

            ByteBuffer byteBuffer = ByteBuffer.wrap(dataByteArray);
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
            int dataVersion = byteBuffer.getInt();
            indexAreaOffset = byteBuffer.getInt();
            phoneRecordCount = (dataByteArray.length - indexAreaOffset) / INDEX_SEGMENT_LENGTH;

        }
    }

    /**
     * 电话归属地查询
     *
     * @param number 电话号码
     */
    public String getAddress(String number) {
        String address = "未知";
        try {
            // 判断是否是手机号码
            // 1[3-8]+9数字
            // 正则表达式
            // ^1[3-8]\d{9}$
            if (number.matches("^1[3-9]\\d{9}$")) {// 匹配是否是手机号码

                address = lookupPhoneNumber(number);

            } else if (specialnum.containsKey(number)) {
                address = specialnum.get(number);
            } else {
                switch (number.length()) {
                    case 5:
                        address = "客服电话";
                        break;
                    case 7:
                    case 8:
                        // 8888 8888
                        address = "本地电话";
                        break;
                    default:
                        // 010 8888 888
                        // 0910 8888 8888
                        if (number.startsWith("0") && number.length() >= 10
                                && number.length() <= 12) {
                            // 有可能是长途电话
                            // 区号是4位的情况
                            String subOne = number.substring(0, 4);
                            String subTwo = number.substring(0, 3);
                            Log.i(TAG, "subOne:" + subOne + "; subTwo:" + subTwo);
                            Scanner scanner = new Scanner(BtApplication.getApplication()
                                    .getAssets().open("dialling-code.txt"));
                            while (scanner.hasNext()) {
                                String nextstr = scanner.next();
                                Log.i(TAG, "next:" + nextstr);
                                if (subOne.equals(nextstr) || subTwo.equals(nextstr)) {
                                    address = scanner.next();
                                    break;
                                } else {
                                    scanner.next();
                                }
                            }

                            if (!"未知".equals(address)) {
                                return address.replaceAll("/", "");
                            }
                        }
                        break;
                }
            }
        } catch (SQLiteDatabaseCorruptException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return address;
    }

    private String lookupPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() > 11 || phoneNumber.length() < 7) {
            return "未知";
        }
        int phoneNumberPrefix;
        try {
            phoneNumberPrefix = Integer.parseInt(phoneNumber.substring(0, 7));
        } catch (Exception e) {
            return "未知";
        }
        int left = 0;
        int right = phoneRecordCount;
        while (left <= right) {
            int middle = (left + right) >> 1;
            int currentOffset = indexAreaOffset + middle * INDEX_SEGMENT_LENGTH;
            if (currentOffset >= dataByteArray.length) {
                return "未知";
            }

            byteBuffer.position(currentOffset);
            int currentPrefix = byteBuffer.getInt();
            if (currentPrefix > phoneNumberPrefix) {
                right = middle - 1;
            } else if (currentPrefix < phoneNumberPrefix) {
                left = middle + 1;
            } else {
                int infoBeginOffset = byteBuffer.getInt();
                int phoneType = byteBuffer.get();

                int infoLength = -1;
                for (int i = infoBeginOffset; i < indexAreaOffset; ++i) {
                    if (dataByteArray[i] == 0) {
                        infoLength = i - infoBeginOffset;
                        break;
                    }
                }

                String infoString = new String(dataByteArray, infoBeginOffset, infoLength, StandardCharsets.UTF_8);
                String[] infoSegments = infoString.split("\\|");


                String result = infoSegments[1] + PHONE_NUMBER_TYPE[phoneType];

                if (TextUtils.isEmpty(result)) {
                    return "未知";
                } else {
                    return result;
                }
            }
        }
        return "未知";
    }
}
