package org.scienty.rest.fileupload;

import java.util.List;

import org.scienty.fileupload.dao.UploadOrderDao;
import org.scienty.fileupload.model.UploadOrder;

public class OrderServiceImpl implements OrderService {
	private UploadOrderDao dao;
	
	public OrderServiceImpl() {
		dao = new UploadOrderDao();
	}
	
	@Override
	public List<UploadOrder> getTickets() {
		return dao.getActiveOrders();
	}

	/* (non-Javadoc)
	 * @see org.scienty.rest.fileupload.OrderService#getOrder()
	 */
	@Override
	public UploadOrder getOrder() {
		return null;
	}
}
