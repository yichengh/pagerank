package worker;

import java.util.ArrayList;
import java.util.HashMap;

import share.Graph;
import share.SharedFunc;

public class WorkerPr {
	public int WorkerId;
	public ArrayList ids = new ArrayList();
	public HashMap idsmp = new HashMap();
	public ArrayList edges = new ArrayList();
	public ArrayList Pr = new ArrayList();
	public ArrayList nPr = new ArrayList();
	public ArrayList nids = new ArrayList();
	public int WorkerNum;
	public Graph g;

	public WorkerPr(Graph g_, int WorkerNum_) throws Exception {
		g = g_;
		WorkerId = g.WorkNo;
		WorkerNum = WorkerNum_;
		int cnt = 0;
		for (int i = 0; i < g.N; i++)
			if (g.isVaild(i, WorkerNum)) {
				ids.add(i);
				Pr.add(1.0 / g.N);
				idsmp.put(i, cnt);
				cnt++;
				ArrayList e = new ArrayList();
				edges.add(e);
			}
		System.out.println(Pr.size() + "," + ids.size());
		for (int i = 0; i < g.M; i++) {
			int x = g.getX(i);
			if (g.isVaild(x, WorkerNum)) {
				// if(!idsmp.containsKey(x))System.out.println("idsmp not
				// exists: "+ x);
				int idx = (int) idsmp.get(x);
				ArrayList e = (ArrayList) (edges.get(idx));
				e.add(g.getY(i));
			}
		}
		// SharedFunc.WriteCheckpoint("checkpoint", Worker.round, ids, Pr);
		// System.out.println(ids);
		// System.out.println(((ArrayList)edges.get(0)).size());
		// System.out.println(((ArrayList)edges.get(1)).size());
	}

	public int setRound() throws Exception {
		System.out.println("master say to reset to " + Worker.masterRound);
		if (Worker.round != Worker.masterRound) {
			SharedFunc.ReadCheckpoint("checkpoint", Worker.masterRound - 1, ids, Pr);
			Worker.round = Worker.masterRound;
		}
		clearMsg();
		return 1;
	}

	public int saveCheckPoint() throws Exception {
		SharedFunc.WriteCheckpoint("checkpoint", Worker.round, ids, Pr);
		return 0;
	}
	
	public int clearMsg(){
		nPr.clear();
		nids.clear();
		return 0;
	}

	public int calcPr() throws Exception {
		for (int i = 0; i < ids.size(); i++) {
			Pr.set(i, 0.15 / g.N);
		}
		for (int i = 0; i < (int) nids.size(); i++) {
			int idx = (int) idsmp.get((int) nids.get(i));
			double cur = (double) Pr.get(idx);
			Pr.set(idx, cur + 0.85 * (double) nPr.get(i));
		}
		clearMsg();
		return 1;
	}

	public synchronized void addMsg(double pr, int idx) {
		nPr.add(pr);
		nids.add(idx);
	}

	public synchronized void addMsg(ArrayList prs, ArrayList idxs) {
		for (int i = 0; i < prs.size(); i++) {
			addMsg((double)prs.get(i),(int)idxs.get(i));
		}
	}

	public void print(int round) {
		System.out.println("round " + round + "finished calc, output pr");
		for (int i = 0; i < ids.size(); i++) {
			int idx = (int) ids.get(i);
			if (idx % 1000 == 0)
				System.out.println("id: " + ids.get(i) + ",pr: " + Pr.get(i));
		}
	}
}
