package worker;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.ArrayList;

import master.Master;
import master.MasterFuncImp;
import share.Graph;
import share.MasterFunc;
import share.SharedFunc;
import share.WorkerFunc;

public class Worker {
	public static WorkerPr wpr;
	public static boolean sendMsgFlag;
	public static String[] workerUrls;
	public static int[] workerIds;
	public static boolean calcPrFlag;
	public static int round;
	public static int masterRound;

	public synchronized int sendPrMsg() throws Exception {
		for (int i = 0; i < workerIds.length; i++) {

			int id = workerIds[i];
			String url = workerUrls[i];

			System.out.println("sendPrMsg:     " + " workerid: " + id + ", workerurl: " + url);

			WorkerFunc send = (WorkerFunc) Naming.lookup(url);

			for (int j = 0; j < Worker.wpr.ids.size(); j++) {
				int idx = (int) Worker.wpr.ids.get(j);
				double pr = ((double) Worker.wpr.Pr.get(j)) / (int) ((ArrayList) Worker.wpr.edges.get(j)).size();
				for (int k = 0; k < ((ArrayList) Worker.wpr.edges.get(j)).size(); k++) {
					int idy = (int) ((ArrayList) Worker.wpr.edges.get(j)).get(k);
					if (idy % Worker.wpr.WorkerNum == id) {
						// System.out.println(Worker.wpr.WorkerId + " try to
						// send to " + id + ", workerurl: " + url + "node: " +
						// idy);
						if (id != Worker.wpr.WorkerId) {
							send.receivePrMsg(pr, idy);
						} else {
							// this.receivePrMsg(pr,idy);
							wpr.addMsg((double) pr, (int) idy);
							// Worker.wpr.nPr.add((double) pr);
							// Worker.wpr.nids.add((int) idy);
							// System.out.println("receive Msg: "+ "workerid: "+
							// Worker.wpr.WorkerId + ", node pagerank value: " +
							// pr + ", node id: " +idy);

						}
					}
				}
			}
		}
		System.out.println("finished sending");
		return 0;
	}

	public static void main(String[] args) throws Exception {
		ArrayList portList = new ArrayList();
		portList.add(7876);
		// String url = SharedFunc.GetIP("10.2.5.185", portList);
		System.out.println("port: " + portList.get(0));
		String url = "//10.2.5.185:" + portList.get(0) + "/FUNCTION";

		String serverUrl = "//162.105.96.50:8804/SAMPLE-SERVER";
		MasterFunc master = (MasterFunc) Naming.lookup(serverUrl);

		System.out.println("server url: " + serverUrl);

		Graph g = new Graph("p2p-Gnutella08.txt");

		int workerNum = 2;
		int id = master.AddWorker(url);
		g.setWorkNo(id);
		System.out.println("worker id: " + id);

		Worker worker = new Worker();
		worker.wpr = new WorkerPr(g, workerNum);
		worker.wpr.saveCheckPoint();

		try {
			LocateRegistry.createRegistry((int) portList.get(0));
			WorkerFuncImp func = new WorkerFuncImp();
			// ע�ᵽ registry ��
			Naming.rebind(url, func);
			System.out.println("worker server ready");
		} catch (RemoteException re) {
			System.out.println("Exception in FibonacciImpl.main: " + re);
		} catch (MalformedURLException e) {
			System.out.println("MalformedURLException " + e);
		}
		System.out.println("worker page rank begins");

		round = 0;

		worker.wpr.print(round);
		round++;
		while (true) {
			Thread.sleep(100);
			if (worker.sendMsgFlag == true) {
				try {
					worker.wpr.setRound();
					worker.sendPrMsg();
				} catch (Exception e) {
					System.out.println("sendPrMsg failed: " + e);
					e.printStackTrace();
				}
				worker.sendMsgFlag = false;
				System.out.println("sendMsg finished");
				master.Completed(id, MasterFunc.SENT_COMPLETED);
			} else if (worker.calcPrFlag == true) {
				worker.wpr.calcPr();
				worker.wpr.saveCheckPoint();
				worker.calcPrFlag = false;
				System.out.println("calc Pr finished");
				master.Completed(id, MasterFunc.SAVE_COMPLETED);
				worker.wpr.print(round);
				round++;
			}
		}

	}
}
