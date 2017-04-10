package priv.season.coworker;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;

import priv.season.coworker.utils.Utils;
import priv.season.net.tcp.SsProtocol;

public class CwPack implements Serializable {

	private static final long serialVersionUID = 1L;
	public static final String TAG_CENTER_ADDR = "TAG_CENTER_ADDR";;
	
	public static final String TAG_SERVICE_REGISTER = "TAG_SERVICE_REGISTER";
	
	public static final String TAG_JARFILE = "TAG_JARFILE";
	public static final String TAG_JAVAFILE = "TAG_JAVAFILE";
	public static final String TAG_CLASSFILE = "TAG_CLASSFILE";
	public static final String TAG_DLIBFILE = "TAG_DLIBFILE";
	public static final String TAG_RESOUCEFILE = "TAG_RESOURCEFILE";
	
	public static final String TAG_SHARED_DATA = "TAG_SHARED_DATA";
	public static final String TAG_SHARED_DATA_CLEAN = "TAG_SHARED_DATA_CLEAN";
	public static final String TAG_TASK = "TAG_TASK";
	public static final String TAG_TASK_RESULT = "TAG_TASK_RESULT";
	
	public static final String PARAM_CENTER_HOST = "PARAM_CENTER_HOST";
	public static final String PARAM_CENTER_PORT = "PARAM_CENTER_PORT";
	
	public static final String PARAM_PROCESS_THREADS = "PARAM_SERVICE_THREADS";
	public static final String PARAM_PROCESS_SPEED = "PARAM_SERVICE_SPEED";
	
	public static final String PARAM_FILE_NAME = "PARAM_FILE_NAME";
	public static final String PARAM_CLASS_NAME = "PARAM_CLASS_NAME";
	public static final String PARAM_METHOD_NAME = "PARAM_METHOD_NAME";
	
	public static final String PARAM_TASK_ID = "PARAM_TASK_ID";
	public static final String PARAM_TASK_INDEX = "PARAM_TASK_INDEX";
	
	public static final String PARAM_SHARED_DATA_ID = "PARAM_SHARED_DATA_ID";
	public static final String PARAM_SHARED_DATA_LEVEL = "PARAM_SHARED_DATA_LEVEL";
	public static final String DATA_OBJECT = "DATA_OBJECT";
	
			
	public static final String DATA_FILE_DATA = "DATA_FILE_DATA";
	public static final String DATA_METHOD_OBJECT = "DATA_METHOD_OBJECT";
	public static final String DATA_METHOD_PARAMS = "DATA_METHOD_PARAMS";
	public static final String DATA_METHOD_RESULT = "DATA_METHOD_RESULT";
	public static final String DATA_METHOD_EXCEPTION = "DATA_METHOD_EXCEPTION";
	


	protected String tag;
	protected HashMap<String, String> params = new HashMap<String, String>();
	protected HashMap<String, Object> datas = new HashMap<String, Object>();


	public static CwPack getSharedDataFromId(String dataId) {
		CwPack result = new CwPack(CwPack.TAG_SHARED_DATA);
		result.addParam(CwPack.PARAM_SHARED_DATA_ID, dataId);
		return result;
	}
	public CwPack(String tag) {
		this.tag = tag;
	}
	public String tag() {
		return tag;
	}
	public Object getData(String key) {
		return datas.get(key);
	}
	public String getParam(String key) {
		return params.get(key);
	}
	
	public void addData(String key, Object data) {
		datas.put(key, data);
	}

	public void addParam(String key, String value) {
		params.put(key, value);
	}
	
	public byte[] toSspMsg() {
		try {
			return SsProtocol.packMsg(Utils.serializeObject(this));
		} catch (IOException e) {
			return null;
		}
	}

	

	@Override
	public String toString() {
		return tag;
	}
	public static CwPack fromClassFile(File file, String relatedFileName) throws IOException {
		if (!file.isFile())
			return null;
		CwPack pack = new CwPack(CwPack.TAG_CLASSFILE);
		pack.addParam(CwPack.PARAM_FILE_NAME, relatedFileName);
		pack.addData(CwPack.DATA_FILE_DATA, Utils.loadFile(file));

		return pack;
	}

	public static CwPack fromJavaFile(File file, String className) throws IOException {
		if (!file.isFile())
			return null;
		CwPack pack = new CwPack(CwPack.TAG_JAVAFILE);
		pack.addParam(CwPack.PARAM_CLASS_NAME, className);

		pack.addData(CwPack.DATA_FILE_DATA, Utils.loadFile(file));

		return pack;
	}

	public static CwPack fromJarFile(File file) throws IOException {
		if (!file.isFile())
			return null;
		CwPack pack = new CwPack(CwPack.TAG_JARFILE);
		pack.addParam(CwPack.PARAM_FILE_NAME, file.getName());

		pack.addData(CwPack.DATA_FILE_DATA, Utils.loadFile(file));

		return pack;
	}

	public static CwPack fromDlibFile(File file) throws IOException {
		if (!file.isFile())
			return null;
		CwPack pack = new CwPack(CwPack.TAG_DLIBFILE);
		pack.addParam(CwPack.PARAM_FILE_NAME, file.getName());
		pack.addData(CwPack.DATA_FILE_DATA, Utils.loadFile(file));

		return pack;
	}
	
	public static CwPack fromTask(String taskId, int taskIndex, String className, String methodName,
			Object obj, Object... params) {
		CwPack pack = new CwPack(CwPack.TAG_TASK);
		pack.addParam(CwPack.PARAM_TASK_ID, taskId);
		pack.addParam(CwPack.PARAM_TASK_INDEX, new Integer(taskIndex).toString());
		pack.addParam(CwPack.PARAM_CLASS_NAME, className);
		pack.addParam(CwPack.PARAM_METHOD_NAME, methodName);
		pack.addData(CwPack.DATA_METHOD_PARAMS, params);
		pack.addData(CwPack.DATA_METHOD_OBJECT, obj);
		return pack;
	}
	
	public static CwPack fromSharedDataClean(int dataLevel) {
		CwPack pack = new CwPack(CwPack.TAG_SHARED_DATA_CLEAN);
		pack.addParam(CwPack.PARAM_SHARED_DATA_LEVEL, new Integer(dataLevel).toString());
		return pack;
	}

	public static CwPack fromSharedData(String dataId, int dataLevel, Object data) {
		CwPack pack = new CwPack(CwPack.TAG_SHARED_DATA);
		pack.addParam(CwPack.PARAM_SHARED_DATA_ID, dataId);
		pack.addParam(CwPack.PARAM_SHARED_DATA_LEVEL, new Integer(dataLevel).toString());
		pack.addData(CwPack.DATA_OBJECT, data);

		return pack;
	}
}
