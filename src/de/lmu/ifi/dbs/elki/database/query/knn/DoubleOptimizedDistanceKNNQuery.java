package de.lmu.ifi.dbs.elki.database.query.knn;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.distance.DoubleDistanceKNNHeap;
import de.lmu.ifi.dbs.elki.database.ids.distance.DoubleDistanceKNNList;
import de.lmu.ifi.dbs.elki.database.query.distance.PrimitiveDistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDoubleDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;

/**
 * Optimized linear scan query for {@link PrimitiveDoubleDistanceFunction}s.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses PrimitiveDoubleDistanceFunction
 * 
 * @param <O> Object type
 */
public class DoubleOptimizedDistanceKNNQuery<O> extends LinearScanDistanceKNNQuery<O, DoubleDistance> {
  /**
   * Raw distance function.
   */
  PrimitiveDoubleDistanceFunction<O> rawdist;

  /**
   * Constructor.
   * 
   * @param distanceQuery Distance function to use
   */
  @SuppressWarnings("unchecked")
  public DoubleOptimizedDistanceKNNQuery(PrimitiveDistanceQuery<O, DoubleDistance> distanceQuery) {
    super(distanceQuery);
    if (!(distanceQuery.getDistanceFunction() instanceof PrimitiveDoubleDistanceFunction)) {
      throw new UnsupportedOperationException("DoubleOptimizedKNNQuery instantiated for non-PrimitiveDoubleDistanceFunction!");
    }
    rawdist = (PrimitiveDoubleDistanceFunction<O>) distanceQuery.getDistanceFunction();
  }

  @Override
  public DoubleDistanceKNNList getKNNForDBID(DBIDRef id, int k) {
    return getKNNForObjectKNNHeap(relation.get(id), k);
  }

  @Override
  public DoubleDistanceKNNList getKNNForObject(O obj, int k) {
    return getKNNForObjectKNNHeap(obj, k);
  }

  /**
   * Heap-based kNN search.
   * 
   * @param obj Query object
   * @param k Desired number of neighbors
   * @return kNN result
   */
  private final DoubleDistanceKNNList getKNNForObjectKNNHeap(O obj, int k) {
    // Avoid getfield in hot loop:
    final PrimitiveDoubleDistanceFunction<O> rawdist = this.rawdist;
    final Relation<? extends O> relation = this.relation;
    DoubleDistanceKNNHeap heap = DBIDUtil.newDoubleDistanceHeap(k);
    for (DBIDIter iter = relation.iterDBIDs(); iter.valid(); iter.advance()) {
      final double dist = rawdist.doubleDistance(obj, relation.get(iter));
      heap.add(dist, iter);
    }
    return heap.toKNNList();
  }
}
