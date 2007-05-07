/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2004,2005,2006, by EADS CRC

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

import org.jcae.mesh.amibe.traits.TriangleTraitsBuilder;

public class TriangleHE extends Triangle
{
	public TriangleHE(TriangleTraitsBuilder ttb)
	{
		super(ttb);
		adj = new AdjacencyHE();
	}

	public AbstractHalfEdge getAbstractHalfEdge()
	{
		return ((AdjacencyHE) adj).getHalfEdge(0);
	}

	public HalfEdge getHalfEdge()
	{
		return (HalfEdge) ((AdjacencyHE) adj).getHalfEdge(0);
	}

	public void setHalfEdge(HalfEdge e)
	{
		((AdjacencyHE) adj).setHalfEdge(0, e);
	}

}