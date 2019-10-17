package ioserver;

import com.ioserver.bean.*;
import com.ioserver.dll.ClientDataBean;
import com.ioserver.dll.GlobalCilentBean;
import com.ioserver.dll.IOServerAPICilent;
import com.sun.jna.NativeLong;
import com.sun.jna.WString;

import java.util.*;

/**
 * @Author: 蔡翔
 * @Date: 2019/8/19 9:05
 * @Version 1.0
 */
public class IOServerApi {

    IOServerAPICilent client = new IOServerAPICilent();
    Timer timer_getTagValue = new Timer();// 数据获取定时器
    Vector<WString> vecSubscribeTagsName = new Vector<WString>();// 添加的订阅变量
    Vector<WString> vecAsyncReadTagsName = new Vector<WString>();// 添加的异步读变量
    Map<NativeLong, WString> mapIdAndName = new HashMap<NativeLong, WString>();

    public Struct_TagInfo funcGetTagValue(WString TagName) {
        ClientDataBean dataBean = GlobalCilentBean.getInstance().getClientByHandle(client.getHandle());
        Struct_TagInfo structTagValue = dataBean.getTagValueByName(TagName);
        return structTagValue;
    }

    public ClientDataBean getClientDataBean(){
        return GlobalCilentBean.getInstance().getClientByHandle(client.getHandle());
    }

    public int funcAsyncRead(String[] strTagName) {
        if ((funcIsConnect() != 0) || (strTagName.length < 1)) {
            return -1;
        }

        vecAsyncReadTagsName.clear();
        WString[] TagNames = new WString[strTagName.length];
        for (int i = 0; i < TagNames.length; i++) {
            TagNames[i] = new WString(strTagName[i]);
            vecAsyncReadTagsName.add(TagNames[i]);
        }

        int result = client.AsyncReadTagsValueByNames(client.getHandle(), TagNames, TagNames.length, 0);
        return result;
    }

    public Struct_TagInfo[] getAsyncReadValue() {
        if (vecAsyncReadTagsName.size() < 1) {
            return null;
        } else {
            try {
                Thread.sleep(400);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        Struct_TagInfo[] TagValueArray = new Struct_TagInfo[vecAsyncReadTagsName.size()];
        ClientDataBean dataBean = GlobalCilentBean.getInstance().getClientByHandle(client.getHandle());

        for (int i_tag = 0; i_tag < vecAsyncReadTagsName.size(); i_tag++) {
            for (int i_delay = 0; i_delay < 101; i_delay++) {
                if (i_delay == 100) {
                    TagValueArray[i_tag] = null;
                    break;
                }
                if (dataBean.getReadComTagValueByName(vecAsyncReadTagsName.get(i_tag)) != null) {
                    TagValueArray[i_tag] = dataBean.getReadComTagValueByName(vecAsyncReadTagsName.get(i_tag));
                    break;
                }

                try { // 如果数据没回来最多将阻塞1s，如果不喜欢这种阻塞延时，
                    Thread.sleep(10); // 请使用带接口的回调注册函数KSIOJAVAAPIRegisterReadCompleteCallbackFunc
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        return TagValueArray; // 存在null读取的时候忽略
    }

    public WString funcGetTagNameById(int id) {
        NativeLong tagId = new NativeLong(id);
        return mapIdAndName.get(tagId);
    }

    private int funcStoreTagIdAndName() {
        mapIdAndName.clear();
        Struct_TagProperty[] tagProperties = client.BrowserCollectTags(client.getHandle(), new WString(""));
        if (tagProperties.length == 0) {
            return -1;
        }
        for (int i = 0; i < tagProperties.length; i++) {
            mapIdAndName.put(tagProperties[i].TagAccessID, tagProperties[i].TagName);
        }
        return tagProperties.length;
    }

    public Struct_TagInfo_AddName[] funcSyncRead(String[] strTagName) {
        if ((funcIsConnect() != 0) || (strTagName.length < 1)) {
            return null;
        }
        WString[] tagNames = new WString[strTagName.length];
        for (int i = 0; i < strTagName.length; i++) {
            tagNames[i] = new WString(strTagName[i]);
        }
        Struct_TagInfo_AddName[] structTagValue = client.SyncReadTagsValueReturnNames(client.getHandle(), tagNames,
                tagNames.length, 0);
        return structTagValue;
    }

    public int funcSyncWrite(String tagName, String tagValue, int type) {
        if ((funcIsConnect() != 0) || (tagName == null) || (tagValue == null)) {
            return -1;
        }

        WString[] wsTagName = new WString[1];
        wsTagName[0] = new WString(tagName);
        if (type == 1) {
            List<Short> valuelist = new ArrayList<Short>();
            int intValue = Integer.parseInt(tagValue);
            valuelist.add((short) intValue);
            return client.SyncWriteTagsValueByNames(client.getHandle(), valuelist, wsTagName);
        }
        if (type == 2) {
            List<Float> valuelist = new ArrayList<Float>();
            float intValue = Float.parseFloat(tagValue);
            valuelist.add((float) intValue);
            return client.SyncWriteTagsValueByNames(client.getHandle(), valuelist, wsTagName);
        }
        if (type == 3) {
            List<String> valuelist = new ArrayList<String>();
            valuelist.add(tagValue);
            return client.SyncWriteTagsValueByNames(client.getHandle(), valuelist, wsTagName);
        }
        return -2;
    }

    public int funcAsyncWrite(String tagName, String tagValue, int type) {
        if ((funcIsConnect() != 0) || (tagName == null) || (tagValue == null)) {
            return -1;
        }

        WString[] wsTagName = new WString[1];
        wsTagName[0] = new WString(tagName);
        // short <==> 1
        if (type == 1) {
            List<Short> valuelist = new ArrayList<Short>();
            int intValue = Integer.parseInt(tagValue);
            valuelist.add((short) intValue);
            return client.AsyncWriteTagsValueByNames(client.getHandle(), valuelist, wsTagName);
        }
        // Float <==> 2
        if (type == 2) {
            List<Float> valuelist = new ArrayList<Float>();
            float intValue = Float.parseFloat(tagValue);
            valuelist.add((float) intValue);
            return client.AsyncWriteTagsValueByNames(client.getHandle(), valuelist, wsTagName);
        }
        // String <==> 3
        if (type == 3) {
            List<String> valuelist = new ArrayList<String>();
            valuelist.add(tagValue);
            return client.AsyncWriteTagsValueByNames(client.getHandle(), valuelist, wsTagName);
        }

        // 在IOServer 中Disc 对应的就是布尔类型。
        // 1 <==> true;; 0 <=> false
        if (type == 4) {
            List<Boolean> valuelist = new ArrayList<Boolean>();
            if(tagValue.equals("1") ){
                valuelist.add(true);
            }else {
                valuelist.add(false);
            }
            return client.AsyncWriteTagsValueByNames(client.getHandle(), valuelist, wsTagName);
        }
        return -2;
    }
    // 连接函数
    public int funcConnect(String ip, String port) {
        if (client.IOServerConnecton(ip, Integer.parseInt(port))) {
            for (int i = 0; i < 51; i++) {
                if (i == 50) {
                    client.IOServerDisConnect(client.getHandle());
                    return -1;
                }
                if (funcIsConnect() == 0) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    if (client.RegisterReadCompleteCallbackFunc(client.getHandle()) != 0) // 注册异步读回调
                    {
                        client.IOServerDisConnect(client.getHandle());
                        return -1;
                    }
                    if (client.RegisterCollectValueCallbackFunc(client.getHandle()) != 0) // 注册订阅回调(回调函数已经被封装，参数收集在databean中的对应map)
                    {
                        client.IOServerDisConnect(client.getHandle());
                        return -1;
                    }
                    if (funcStoreTagIdAndName() < 0) {
                        client.IOServerDisConnect(client.getHandle());
                        return -1;
                    }
                    break;
                }
            }

            //int tagId[]=new int[1];
            //tagId[0]=5001;
            //client.AsyncReadTagsValueByIDs(client.getHandle(), tagId, 1, 0);
            return 0;
        }
        client.IOServerDisConnect(client.getHandle());
        return -2; // 没有获取到句柄
    }

    //
    public int funcDisConnect() {
        return client.IOServerDisConnect(client.getHandle());
    }

    public Vector<WString> funcSubscribeAllTags() {
        if (funcIsConnect() != 0) {
            return null;
        }
        // 层次化浏览所有变量
        vecSubscribeTagsName.clear();
        Struct_ChannelProperty[] channelProperties = client.BrowserChannels(client.getHandle(), new WString(""));

        for (int i_channel = 0; i_channel < channelProperties.length; i_channel++) {
            Struct_DeviceProperty[] deviceProperties = client.BrowserDevices(client.getHandle(),
                    channelProperties[i_channel].ChannelName);

            for (int i_device = 0; i_device < deviceProperties.length; i_device++) {
                Struct_TagProperty[] tagProperties = client.BrowserCollectTags(client.getHandle(),
                        deviceProperties[i_device].DeviceName);

                WString[] TagNames = new WString[tagProperties.length - 2];
                int i_tagNames = 0;
                for (int i_tag = 0; i_tag < tagProperties.length; i_tag++) {
                    String wTagName = tagProperties[i_tag].TagName.toString();
                    if (wTagName.indexOf(new String("$")) == -1) {
                        TagNames[i_tagNames] = tagProperties[i_tag].TagName;
                        vecSubscribeTagsName.add(tagProperties[i_tag].TagName);
                        i_tagNames++;
                    }
                }
                int[] TagIDs = new int[i_tagNames];
                for (int i = 0; i < i_tagNames; i++) {
                    TagIDs[i] = GlobalCilentBean.getInstance().getTagIDbyName(TagNames[i]);
                }
                client.SubscribeTagValuesChange(client.getHandle(), TagIDs, TagIDs.length);

            }
        }

        return vecSubscribeTagsName;
    }

    public int funcIsConnect() {
        if (client.IOServerIsConnected(client.getHandle()) == true) {
            if (client.getIOServerWorkStatus(client.getHandle()) == 0) {
                return 0;
            }
            else {
                return -1;	//IOServer没启动
            }
        }
        return -2;	//连接断开
    }
}
