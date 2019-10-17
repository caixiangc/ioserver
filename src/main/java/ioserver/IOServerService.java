package ioserver;

import com.ioserver.bean.Struct_TagInfo;
import com.ioserver.bean.Struct_TagInfo_AddName;
import com.ioserver.bean.Union_DataType;
import com.ioserver.dll.ClientDataBean;
import com.sun.jna.WString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * @Author: 蔡翔
 * @Date: 2019/8/28 15:31
 * @Version 1.0
 */
public class IOServerService {
    private static final Logger logger = LoggerFactory.getLogger(IOServerService.class);

    IOServerApi ioServerApi = new IOServerApi();
    Vector<WString> allTag = new Vector<WString>();

    private String ip = IOServerConf.ip;
    private String port = IOServerConf.port;

    /**
     *
     * 连接IOServer
     * return ：0 == 连接成功；  -2 == 获取句柄失败；   -1 == 其他错误
     *
     * */
    public Integer getConnection() throws Exception{
        Integer isConnectd = ioServerApi.funcIsConnect();
        if(isConnectd == 0){
            //如果已近连接直接返回就行。
            return 0;
        }

        Integer result;
        try {
            result = ioServerApi.funcConnect(ip,port);
        }catch (Exception e){
            logger.error(e.toString());
            throw new Exception("IOServer 连接异常请与管理员联系");
        }
        return result;
    }

    public void judgeConnection() throws Exception{
        Integer isConnectd = ioServerApi.funcIsConnect();
        if(isConnectd != 0){
            Integer result = getConnection();
            if(result == -2){
                throw new Exception("连接到IOServer失败,请检查是否开启IOServer");
            }else if(result == -1){
                throw new Exception("连接到IOServer异常，请联系管理员");
            }
        }
    }

    public boolean judgeTagNameIsValid(String tagName) throws Exception{
        if(allTag.size() == 0 && allTag.isEmpty() ==true){
            allTag = getAllTagName();
        }
        for(WString wString : allTag){
            if(wString.toString().equals(tagName)){
                return true;
            }
        }
        return false;
    }


    /**
     *
     * 依据tagName 获取它的tagValue
     *
     * */
    public com.ioserver.bean.Union_DataType.ByValue getValueByName(String tagNameArg) throws Exception{
        judgeConnection();
        if(!judgeTagNameIsValid(tagNameArg)){
            throw new Exception("查IOServer 无此变量名");
        }
        String[] tagName = {tagNameArg};
        Struct_TagInfo_AddName[] struct_tagInfos = ioServerApi.funcSyncRead(tagName);
        if(struct_tagInfos[0] !=null && struct_tagInfos.length > 0){
            com.ioserver.bean.Struct_TagValue.ByValue tagValue = struct_tagInfos[0].TagValue;
            return tagValue.TagValue;
        }else{
            throw new Exception("采集到变量 对应的属性值 为null；请检查设备或者稍后再试");
        }

    }

    /**
     *
     * 依据tagNames 获取它的tagValues
     * tagNames 用逗号隔开
     *
     * */
    public List<Union_DataType.ByValue> getBatchValueByNames(String tagNameArg) throws Exception{
        judgeConnection();

        String[] tagName = tagNameArg.split(",");
        for(String s:tagName){
            if(!judgeTagNameIsValid(s)){
                throw new Exception("有一个tagName不合法：这个变量是："+s);
            }
        }

        Struct_TagInfo_AddName[] struct_tagInfos = ioServerApi.funcSyncRead(tagName);

        List<Union_DataType.ByValue> result = new ArrayList<Union_DataType.ByValue>();
        for(int i = 0;i<tagName.length;i++){
            result.add(struct_tagInfos[i].TagValue.TagValue);
        }
        return result;
    }

    /**
     *
     * 断开连接
     * return
     *
     * */
    public Integer funcDisConnect () throws Exception{
        Integer result;
        try {
            result = ioServerApi.funcDisConnect();
        }catch (Exception e){
            logger.error(e.toString());
            throw new Exception("断开IOServer异常 ，请联系管理员");
        }
        return result;
    }


    /**
     *
     * 获取所有tagName; 订阅所有变量 ，并且监听ClientDataBean
     *
     * */
    public Vector<WString> getAllTagName() throws Exception{
        judgeConnection();

        Vector<WString> vector = ioServerApi.funcSubscribeAllTags();
        ClientDataBean clientDataBean = ioServerApi.getClientDataBean();
        Thread.sleep(500);
        return vector;
    }

    /**
     *
     * 启动线程，监听某一个变量。并且在执行 某些特定的业务
     *
     * */
    public boolean listenTagValueByTagName(String tagName) throws Exception{
        Vector<WString> vector = getAllTagName();
        //todo 启动线程。
        IOServerListenerThread ioServerListenerThread = new IOServerListenerThread(ioServerApi,tagName);
        ioServerListenerThread.start();
        System.out.println(ioServerListenerThread.isAlive());
        boolean flag = ioServerListenerThread.isAlive();
        if(flag){
            return true;
        }else {
            return false;
        }
    }


    /**
     *
     * 依据tagName 和 type  修改 tagValue; 在写入的时候 要注意 tagValue和 type 类型 关系要对应
     * 参数：
     *      tagName - plc里的变量名
     *      tagValue - 你要修改plc变量的值
     *      type - 1 ==》 Short ； 2 ==》 Float ; 3 ==》 String
     *  return：
     *          0 表示成功
     *          -1 表示未连接IOServer 或者tagName、tagValue未空
     *          -2  表示不支持写入数据类型
     * */
    public Integer setTagValue(String tagName, String tagValue, int type) throws Exception{
        judgeConnection();
        int result;
        try {
            result = ioServerApi.funcAsyncWrite(tagName,tagValue,type);
        }catch (Exception e){
            throw new Exception("位置异常请与管理员联系");
        }
        return result;
    }



    /**
     * getSubscribeTagValueByTagName 和 getSubscribeAllTagValue 接口与getAllTagName 接口类似
     * 都是需要调用后，单独开一个 线程去监听，你需要知道的变量。
     **/

    /**
     *
     * 流程：先订阅所有变量（相当于注册到IOServer上），然后再 再通过funcGetTagValue 去取某个特定的。
     * 订阅变量（订阅全部变量）
     * 参数:
     *      tagName - 是你要订阅的变量名
     * */
    public Struct_TagInfo[] getSubscribeAllTagValue() throws Exception{
        judgeConnection();
        Vector<WString> vecAllTagName = new Vector<WString>();
        vecAllTagName = ioServerApi.funcSubscribeAllTags();
        Thread.sleep(500);
        Struct_TagInfo[] result = new Struct_TagInfo[vecAllTagName.size()];
        for(int i=0;i<vecAllTagName.size();i++){
            WString tagName = vecAllTagName.get(i);
            if(tagName != null){
                result[i] = ioServerApi.funcGetTagValue(tagName);
            }
        }
        return result;
    }

    /**
     *
     * 订阅变量（订阅某一个变量）
     * 参数:
     *      tagName - 是你要订阅的变量名
     * */
    public Struct_TagInfo getSubscribeTagValueByTagName(String tagName) throws Exception{
        judgeConnection();
        Integer isConnectd = ioServerApi.funcIsConnect();
        Vector<WString> vecAllTagName = new Vector<WString>();
        vecAllTagName = ioServerApi.funcSubscribeAllTags();
        WString wString = new WString(tagName);
        //WString wString = vecAllTagName.get(0);
        Thread.sleep(500);
        Struct_TagInfo result = ioServerApi.funcGetTagValue(wString);
        return result;
    }


}
