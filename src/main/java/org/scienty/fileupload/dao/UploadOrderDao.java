package org.scienty.fileupload.dao;

import java.util.ArrayList;
import java.util.List;

import org.scienty.fileupload.model.OrderState;
import org.scienty.fileupload.model.UploadOrder;

public class UploadOrderDao {

	public UploadOrderDao() {
		
	}

	public List<UploadOrder> getActiveOrders() {
		List<UploadOrder> retList = new ArrayList<>();
		UploadOrder order = new UploadOrder("ID1", "Prachi", OrderState.ORDERED.toString(), 0, 0.1, "Note", "Order confirmed");
		retList.add(order);
		order = new UploadOrder("ID2", "Aryan", OrderState.UPLOAD.toString(), 0, 0.4, "Note1", "Please upload files");
		retList.add(order);
		order = new UploadOrder("ID3", "Sanvi", OrderState.UPLOAD.toString(), 0, 0.5, "Note3", "Upload in progress");
		retList.add(order);
		order = new UploadOrder("ID4", "Ananya", OrderState.UPLOAD.toString(), 0, 0.6, "Note3", "Upload in progress");
		retList.add(order);
		order = new UploadOrder("ID4", "Yukthi", OrderState.UPLOAD.toString(), 0, 0.7, "Note3", "Upload in progress");
		retList.add(order);
		
		return retList;
	}
}
