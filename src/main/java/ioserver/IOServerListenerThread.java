package ioserver;

import com.sun.jna.WString;

import java.util.Date;

/**
 * @Author: 蔡翔
 * @Date: 2019/9/9 19:20
 * @Version 1.0
 *
 *
 */
public class IOServerListenerThread extends Thread{
    private static Object temp = null;
    private static boolean flag = false;
    private String tagName;
    private IOServerApi ioServerApi;
    private com.ioserver.bean.Union_DataType.ByValue currentDate;
    public IOServerListenerThread(IOServerApi ioServerApi, String tagName){
        this.ioServerApi = ioServerApi;
        this.tagName = tagName;
        temp = ioServerApi.funcGetTagValue(new WString(tagName)).TagValue.TagValue;
    }


    @Override
    public void run() {
        WString wString = new WString(tagName);

        while (true){
            currentDate = ioServerApi.funcGetTagValue(wString).TagValue.TagValue;
            //todo 1  这里写判断逻辑
            if(!temp.equals(currentDate)){
                // todo  不相等，发生改变的时候，调用 你要执行的函数。
                //业务逻辑   ---开始
                System.err.println("监听到 数据改变在，时间："+new Date());
                   //业务逻辑   ---结束
                temp = currentDate;
            }
            try {
                Thread.sleep(300);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
}
