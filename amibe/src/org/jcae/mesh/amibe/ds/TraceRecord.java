/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2011, by EADS France

    This library is free software; you can redistribute it and/or
    modify it under the terms of the GNU Lesser General Public
    License as published by the Free Software Foundation; either
    version 2.1 of the License, or (at your option) any later version.

    This library is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public
    License along with this library; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA */

package org.jcae.mesh.amibe.ds;

import gnu.trove.TIntObjectHashMap;
import gnu.trove.TObjectIntHashMap;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

import java.util.logging.Level;
import java.util.logging.Logger;

public class TraceRecord implements TraceInterface
{
        private static final Logger LOGGER=Logger.getLogger(TraceRecord.class.getName());
	private static final String MANY_SPACES = "                                                                                  ";
	private String logName;
	private PrintStream out = System.out;
	private PrintStream mainOut = System.out;
	private String meshVariable = "mesh";
	private boolean disabled;
	private int indentLevel;
	private String tab = "";
	private int cntLines;
	private int cntMethods;
	private int cntClasses;

	private int labelVertex;
	private final TObjectIntHashMap<Vertex> mapVertexId = new TObjectIntHashMap<Vertex>();
	private final TIntObjectHashMap<Vertex> mapIdVertex = new TIntObjectHashMap<Vertex>();

	private int labelTriangle;
	private final TObjectIntHashMap<Triangle> mapTriangleId = new TObjectIntHashMap<Triangle>();
	private final TIntObjectHashMap<Triangle> mapIdTriangle = new TIntObjectHashMap<Triangle>();

	public void setLogFile(String logName)
	{
		this.logName = logName;
		try {
			out = new PrintStream(new FileOutputStream(logName+".py"));
		} catch (FileNotFoundException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		}
		mainOut = out;
	}

	public void createMesh(String meshName, Mesh mesh)
	{
		meshVariable = meshName;
		println(mainOut, "import org.jcae.mesh.amibe.traits.MeshTraitsBuilder");
		println(mainOut, "import org.jcae.mesh.xmldata.MeshWriter");
		println(mainOut, "import org.jcae.mesh.amibe.ds.Mesh");

		println(mainOut, "cntModule = 0");
		println(mainOut, "mods = []");
		println(mainOut, "while True:");
		startScope();
		println(mainOut, "try:");
		startScope();
		println(mainOut, "mods.append(__import__(\""+logName+"_cl%d\" % cntModule))");
		println(mainOut, "cntModule += 1");
		endScope();
		println(mainOut, "except ImportError:");
		startScope();
		println(mainOut, "break");
		endScope();
		endScope();

		println(mainOut, "mtb = org.jcae.mesh.amibe.traits.MeshTraitsBuilder.getDefault3D()");
		println(mainOut, "mtb.addTraceReplay()");
		println(mainOut, "mtb.addNodeSet()");
		println(mainOut, meshVariable+" = org.jcae.mesh.amibe.ds.Mesh(mtb)");
		println(mainOut, "mods[0].c("+meshVariable+")");

		try {
			out = new PrintStream(new FileOutputStream(logName+"_cl"+cntClasses+".py"));
		} catch (FileNotFoundException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		}

		println("class c():");
		startScope();
		println("def __init__(self, m):");
		startScope();
		println("self.m = m");

		mapIdVertex.put(-1, mesh.outerVertex);
		mapVertexId.put(mesh.outerVertex, -1);
		println("self.m.getTrace().add(self.m.outerVertex, -1)");
		for (Vertex v : mesh.getNodes())
		{
			createAndAdd(v);
			println("self.m.add(v)");
			checkLines();
		}
		for (Triangle t : mesh.getTriangles())
		{
			if (!t.hasAttributes(AbstractHalfEdge.OUTER))
			{
				createAndAdd(t);
				println("self.m.add(t)");
				checkLines();
			}
		}
		if (mesh.hasAdjacency())
			println("self.m.buildAdjacency()");
		checkLines();
	}

	public void setDisabled(boolean b)
	{
		disabled = b;
	}

	public boolean getDisabled()
	{
		return disabled;
	}

	private void createAndAdd(Vertex v)
	{
		add(v);
		double[] pos = v.getUV();
		println("v = self.m.createVertex("+pos[0]+","+pos[1]+","+pos[2]+")");
		println("self.m.getTrace().add(v, "+labelVertex+")");
	}

	public void add(Vertex v)
	{
		labelVertex++;
		mapIdVertex.put(labelVertex, v);
		mapVertexId.put(v, labelVertex);
	}

	public void add(Vertex v, int id)
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public void remove(Vertex v)
	{
		int id = mapVertexId.get(v);
		println("self.m.getTrace().remove(self.m.getTrace().getVertex("+id+"))");
		mapIdVertex.remove(id);
		mapVertexId.remove(v);
	}

	public Vertex getVertex(int id)
	{
		return mapIdVertex.get(id);
	}

	public int getVertexId(Vertex v)
	{
		return mapVertexId.get(v);
	}

	private void createAndAdd(Triangle t)
	{
		add(t);
		println("vTemp0 = self.m.getTrace().getVertex("+mapVertexId.get(t.vertex[0])+")");
		println("vTemp1 = self.m.getTrace().getVertex("+mapVertexId.get(t.vertex[1])+")");
		println("vTemp2 = self.m.getTrace().getVertex("+mapVertexId.get(t.vertex[2])+")");
		println("t = self.m.createTriangle(vTemp0, vTemp1, vTemp2)");
		println("t.setGroupId("+t.getGroupId()+")");
		if (!t.isReadable())
			println("t.setReadable(False)");
		if (!t.isWritable())
			println("t.setWritable(False)");
		println("self.m.getTrace().add(t, "+labelTriangle+")");
	}

	public void add(Triangle t)
	{
		labelTriangle++;
		mapIdTriangle.put(labelTriangle, t);
		mapTriangleId.put(t, labelTriangle);
	}

	public void add(Triangle t, int id)
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public void remove(Triangle t)
	{
		int id = mapTriangleId.get(t);
		println("self.m.getTrace().remove(self.m.getTrace().getTriangle("+id+"))");
		mapIdTriangle.remove(id);
		mapTriangleId.remove(t);
	}

	public Triangle getTriangle(int id)
	{
		return mapIdTriangle.get(id);
	}

	public int getTriangleId(Triangle t)
	{
		return mapTriangleId.get(t);
	}

	public void addAdjacentTriangles(Mesh m)
	{
		if (disabled)
			return;
		for (Triangle t : m.getTriangles())
		{
			if (!t.hasAttributes(AbstractHalfEdge.BOUNDARY | AbstractHalfEdge.NONMANIFOLD))
				continue;
			if (t.hasAttributes(AbstractHalfEdge.OUTER))
				continue;
			AbstractHalfEdge ot = t.getAbstractHalfEdge();
			println("t = self.m.getTrace().getTriangle("+mapTriangleId.get(t)+")");
			println("ot = t.getAbstractHalfEdge()");
			println("for i in xrange(3):");
			startScope();
			for (int i = 0; i < 3; i++)
			{
				ot = ot.next();
				println("ot = ot.next()");
				if (ot.hasAttributes(AbstractHalfEdge.BOUNDARY | AbstractHalfEdge.NONMANIFOLD))
				{
					Triangle s = ot.sym().getTri();
					if (!mapTriangleId.contains(s))
						add(s);
					println("self.m.getTrace().add(ot.sym().getTri(), "+mapTriangleId.get(s)+")");
				}
			}
			endScope();
			checkLines();
		}
	}

	private void startScope()
	{
		indentLevel += 1;
		tab = MANY_SPACES.substring(0, 4*indentLevel);
	}

	private void endScope()
	{
		indentLevel -= 1;
		tab = MANY_SPACES.substring(0, 4*indentLevel);
	}

	public void edgeSwap(AbstractHalfEdge h)
	{
		if (disabled)
			return;
		println("t = self.m.getTrace().getTriangle("+mapTriangleId.get(h.getTri())+")");
		println("ot = t.getAbstractHalfEdge()");
		if (h.getLocalNumber() == 1)
			println("ot = ot.next()");
		else if(h.getLocalNumber() == 2)
			println("ot = ot.prev()");
		println("self.m.edgeSwap(ot)");
		checkLines();
	}

	public void edgeCollapse(AbstractHalfEdge h, Vertex v)
	{
		if (disabled)
			return;
		createAndAdd(v);
		remove(h.origin());
		remove(h.destination());
		println("t = self.m.getTrace().getTriangle("+mapTriangleId.get(h.getTri())+")");
		println("ot = t.getAbstractHalfEdge()");
		if (h.getLocalNumber() == 1)
			println("ot = ot.next()");
		else if(h.getLocalNumber() == 2)
			println("ot = ot.prev()");
		println("self.m.edgeCollapse(ot, self.m.getTrace().getVertex("+mapVertexId.get(v)+"))");
		checkLines();
	}

	public void vertexSplit(AbstractHalfEdge h, Vertex v)
	{
		if (disabled)
			return;
		createAndAdd(v);
		println("t = self.m.getTrace().getTriangle("+mapTriangleId.get(h.getTri())+")");
		println("ot = t.getAbstractHalfEdge()");
		if (h.getLocalNumber() == 1)
			println("ot = ot.next()");
		else if(h.getLocalNumber() == 2)
			println("ot = ot.prev()");
		println("ot = self.m.vertexSplit(ot, self.m.getTrace().getVertex("+mapVertexId.get(v)+"))");
		if (h.hasSymmetricEdge())
		{
			add(h.sym().getTri());
			println("self.m.getTrace().add(ot.sym().getTri(), "+mapTriangleId.get(h.sym().getTri())+")");
		}
		if (h.next().hasSymmetricEdge())
		{
			add(h.next().sym().getTri());
			println("self.m.getTrace().add(ot.next().sym().getTri(), " + mapTriangleId.get(h.next().sym().getTri()) + ")");
		}
		checkLines();
	}

	private void checkLines()
	{
		if (cntLines > 40)
		{
			out.println(tab+"self.cont"+cntMethods+"()\n");
			if (indentLevel > 0)
				endScope();
			out.println(tab+"def cont"+cntMethods+"(self):");
			startScope();
			cntLines = 0;
			cntMethods++;
			checkMethods();
		}
	}

	private void checkMethods()
	{
		if (cntMethods > 100)
		{
			out.println(tab+"pass");
			cntClasses++;
			endScope();
			endScope();
			out.close();
			mainOut.println("mods["+cntClasses+"].c("+meshVariable+")");
			try {
				out = new PrintStream(new FileOutputStream(logName+"_cl"+cntClasses+".py"));
			} catch (FileNotFoundException ex) {
				LOGGER.log(Level.SEVERE, null, ex);
			}

			out.println(tab+"class c():");
			startScope();
			out.println(tab+"def __init__(self, m):");
			startScope();
			out.println(tab+"self.m = m");
			cntMethods = 0;
		}
	}

	public void println(String x)
	{
		println(out, x);
	}

	private void println(PrintStream o, String x)
	{
		if (!disabled)
		{
			o.println(tab+x);
			cntLines++;
		}
	}

	public void printMeshln(String x)
	{
		println(meshVariable+x);
	}

	public void finish()
	{
		cntClasses++;
		endScope();
		endScope();
		out.close();
		println(mainOut, "org.jcae.mesh.xmldata.MeshWriter.writeObject3D("+meshVariable+
			", \""+logName+"-out\""+", None)");
		mainOut.close();
	}

}
