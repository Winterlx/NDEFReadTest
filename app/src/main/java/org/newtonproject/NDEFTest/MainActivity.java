package org.newtonproject.NDEFTest;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import com.nxp.nfclib.NxpNfcLib;

import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;

import java.util.Arrays;

@SuppressLint("Registered")
@EActivity
public class MainActivity extends AppCompatActivity {

    private NxpNfcLib m_libInstance = null;
    private String m_strKey = "046aceb8197c956108c6e7869c8d3c8d";

    @ViewById
    TextView textView;

    private String mTagText;
    private String tag = "test";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.e(tag, "onCreate: ");
        initializeLibrary();

     }

    private void readNFCTag(Intent intent) {

        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
        Tag detectedTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        //2.获取Ndef的实例
        Ndef ndef = Ndef.get(detectedTag);
        mTagText = "type:" + ndef.getType();
        mTagText += "\nmaxsize:" + ndef.getMaxSize() + "bytes";
        readNfcTag(intent);
        textView.setText(mTagText);
    }

    private void readNfcTag(Intent intent) {
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
            Parcelable[] rawMsgs = intent.getParcelableArrayExtra(
                    NfcAdapter.EXTRA_NDEF_MESSAGES);
            NdefMessage msgs[] = null;
            int contentSize = 0;
            if (rawMsgs != null) {
                msgs = new NdefMessage[rawMsgs.length];
                for (int i = 0; i < rawMsgs.length; i++) {
                    msgs[i] = (NdefMessage) rawMsgs[i];
                    contentSize += msgs[i].toByteArray().length;
                }
            }
            try {
                if (msgs != null) {
                    NdefRecord record = msgs[0].getRecords()[0];
                    String textRecord = parseTextRecord(record);
                    mTagText += "\ncontent:" + textRecord;
                    mTagText += "\ncontentSize:" + contentSize + " bytes";
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 解析NDEF文本数据，从第三个字节开始，后面的文本数据
     *
     * @param ndefRecord
     * @return
     */
    public static String parseTextRecord(NdefRecord ndefRecord) {
        /**
         * 判断数据是否为NDEF格式
         */
        //判断TNF
        if (ndefRecord.getTnf() != NdefRecord.TNF_WELL_KNOWN) {
            return null;
        }
        //判断可变的长度的类型
        if (!Arrays.equals(ndefRecord.getType(), NdefRecord.RTD_TEXT)) {
            return null;
        }
        try {
            //获得字节数组，然后进行分析
            byte[] payload = ndefRecord.getPayload();
            //下面开始NDEF文本数据第一个字节，状态字节
            //判断文本是基于UTF-8还是UTF-16的，取第一个字节"位与"上16进制的80，16进制的80也就是最高位是1，
            //其他位都是0，所以进行"位与"运算后就会保留最高位
            String textEncoding = ((payload[0] & 0x80) == 0) ? "UTF-8" : "UTF-16";
            //3f最高两位是0，第六位是1，所以进行"位与"运算后获得第六位
            int languageCodeLength = payload[0] & 0x3f;
            //下面开始NDEF文本数据第二个字节，语言编码
            //获得语言编码
            String languageCode = new String(payload, 1, languageCodeLength, "US-ASCII");
            //下面开始NDEF文本数据后面的字节，解析出文本
            String textRecord = new String(payload, languageCodeLength + 1,
                    payload.length - languageCodeLength - 1, textEncoding);
            return textRecord;
        } catch (Exception e) {
            throw new IllegalArgumentException();
        }
    }

    private void initializeLibrary() {
        m_libInstance = NxpNfcLib.getInstance();
        m_libInstance.registerActivity(this, m_strKey);
    }

    @Override
    protected void onResume() {
        m_libInstance.startForeGroundDispatch();
        super.onResume();
    }

    @Override
    protected void onPause() {
        m_libInstance.stopForeGroundDispatch();
        super.onPause();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        readNFCTag(intent);
        super.onNewIntent(intent);
    }

    /*private NdefRecord createNdefRecord() {

        String ssid = ssid_text.getText().toString();
        String psk = pwd_text.getText().toString();

        ssid = "ssid=\"" + ssid + "\"";
        psk = "psk=\"" + psk + "\"";

        String text = ssid + psk;

        String lang = "zh";
        byte[] textBytes = text.getBytes();
        byte[] langBytes = lang.getBytes(StandardCharsets.UTF_8);
        int langLength = langBytes.length;
        int textLength = textBytes.length;

        byte[] payload = new byte[1 + langLength + textLength];
        payload[0] = (byte) langLength;

        System.arraycopy(langBytes, 0, payload, 1, langLength);
        System.arraycopy(textBytes, 0, payload, 1 + langLength, textLength);

        return new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[0], payload);
    }*/

    /*private void writeNDEFMessage(Tag tag) throws IOException, FormatException {

        NdefRecord[] records = {createNdefRecord()};
        NdefMessage message = new NdefMessage(records);
        Ndef ndef = Ndef.get(tag);
        ndef.connect();
        ndef.writeNdefMessage(message);
        Toast.makeText(this, "Succeed to write wifi configuration.", Toast.LENGTH_SHORT).show();
        ndef.close();

    }*/

}
