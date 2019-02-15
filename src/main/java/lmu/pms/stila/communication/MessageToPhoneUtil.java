package lmu.pms.stila.communication;

import android.content.Context;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import org.json.JSONObject;

import java.util.HashSet;
import java.util.List;

import lmu.pms.stila.SysConstants.CommunicationConstants;

public class MessageToPhoneUtil {

    /**
     * Describes the category of the message
     */
    public enum MessageMode{
        CONFIRM_ONLINE
    }


    private Context mContext;
    public MessageToPhoneUtil(Context context){
        mContext = context;

    }

    public void sendMessage(final MessageMode mode, final String [] content) {

        List<Node> nodes =
                null;
        Wearable.getNodeClient(mContext).getConnectedNodes().addOnSuccessListener(new OnSuccessListener<List<Node>>() {
            @Override
            public void onSuccess(List<Node> nodes) {
                HashSet<String> results = new HashSet<String>();
                for (Node node : nodes) {
                    results.add(node.getId());


                    if (mode == MessageMode.CONFIRM_ONLINE) {
                        for (String discoverNode : results) {
                            Task<Integer> sendTask =
                                    Wearable.getMessageClient(mContext).sendMessage(
                                            discoverNode, CommunicationConstants.WEAR_PATH+CommunicationConstants.CONFIRM_ONLINE_PATH, null);
                        }
                    }

                }
            }
        });


    }
}
