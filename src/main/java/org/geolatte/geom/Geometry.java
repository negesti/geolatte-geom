/*
 * This file is part of the GeoLatte project.
 *
 *     GeoLatte is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     GeoLatte is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with GeoLatte.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2010 - 2011 and Ownership of code is shared by:
 * Qmino bvba - Romeinsestraat 18 - 3001 Heverlee  (http://www.qmino.com)
 * Geovise bvba - Generaal Eisenhowerlei 9 - 2140 Antwerpen (http://www.geovise.com)
 */

package org.geolatte.geom;

import org.geolatte.geom.codec.Wkt;
import org.geolatte.geom.crs.CoordinateReferenceSystem;

import java.io.Serializable;
import java.lang.reflect.Array;

/**
 * The base class for <code>Geometry</code>s.
 *
 * @author Karel Maesen, Geovise BVBA, 2011
 */

public abstract class Geometry<P extends Position> implements Serializable {

    private static GeometryEquality geomEq = new GeometryPointEquality();

    protected final PositionSequence<P> positions;

    @SuppressWarnings("unchecked")
    public static <Q extends Position> Geometry<Q> forceToCrs(Geometry<?> geometry, CoordinateReferenceSystem<Q> crs) {
        if (crs == null || geometry == null) return (Geometry<Q>) geometry;
        if (crs.equals(geometry.getCoordinateReferenceSystem())) return (Geometry<Q>) geometry;
        if (geometry instanceof Simple) {
            Simple simple = (Simple) geometry;
            PositionSequence<Q> positions = Positions.copy(geometry.getPositions(), crs);
            return Geometries.mkGeometry(simple.getClass(), positions);
        } else {
            Complex<?, ?> complex = (Complex<?, ?>) geometry;
            if (complex.getNumGeometries() == 0) {
                return Geometries.mkGeometry(complex.getClass(), crs);
            }
            Geometry<Q>[] targetParts = (Geometry<Q>[]) Array.newInstance(complex.getComponentType(), complex.getNumGeometries());//new Geometry[complex.getNumGeometries()];
            int idx = 0;
            for (Geometry<?> part : complex) {
                targetParts[idx++] = forceToCrs(part, crs);
            }
            return Geometries.mkGeometry(complex.getClass(), targetParts);
        }
    }

    /**
     * Creates an empty Geometry
     *
     * @param crs the CoordinateReferenceSystem to use
     */
    protected Geometry(CoordinateReferenceSystem<P> crs) {
        this.positions = PositionSequenceBuilders.fixedSized(0, crs).toPositionSequence();
    }

    protected Geometry(PositionSequence<P> positions) {
        if (positions == null) throw new IllegalArgumentException("Null Positions argument not allowd.");
        this.positions = positions;
    }

    @SuppressWarnings("unchecked")
    protected static <T extends Position> PositionSequence<T> nestPositionSequences(Geometry<T>[] geometries) {
        if (geometries == null || geometries.length == 0) {
            return null;
        }
        PositionSequence<T>[] sequences = (PositionSequence<T>[]) (new PositionSequence[geometries.length]);
        int i = 0;
        for (Geometry<T> g : geometries) {
            sequences[i++] = g.getPositions();
        }
        return new NestedPositionSequence<T>(sequences);
    }

    //TODO -- check that all geoms have the SAME CRS
    @SuppressWarnings("unchecked")
    protected static <T extends Position> CoordinateReferenceSystem<T> getCrs(Geometry<T>[] geometries) {
        if (geometries == null || geometries.length == 0) {
            throw new IllegalArgumentException("Expecting non-null, non-empty array of Geometry.");
        }
        return geometries[0].getCoordinateReferenceSystem();
    }


    /**
     * Returns the coordinate dimension of this <code>Geometry</code>
     * <p/>
     * <p>The coordinate dimension is the number of components in the coordinates of the points in
     * this <code>Geometry</code>. </p>
     *
     * @return the coordinate dimension
     */
    public int getCoordinateDimension() {
        return getPositions().getCoordinateDimension();
    }

    /**
     * Returns the coordinate reference system of this <code>Geometry</code>
     *
     * @return
     */
    public CoordinateReferenceSystem<P> getCoordinateReferenceSystem() {
        return getPositions().getCoordinateReferenceSystem();
    }

    /**
     * Returns the numeric identifier of the coordinate reference system of this <code>Geometry</code>.
     * <p/>
     * <p>A SRID is usually interpreted as meaning the EPSG-code for the coordinate reference system. In this
     * implementation, this is not enforced.</p>
     *
     * @return
     */
    public int getSRID() {
        return getCoordinateReferenceSystem().getCrsId().getCode();
    }

    /**
     * Tests whether this <code>Geometry</code> corresponds to the empty set.
     *
     * @return
     */
    public boolean isEmpty() {
        return this.getPositions().isEmpty();
    }

    /**
     * Returns the number of points in the <code>PointSequence</code> of this <code>Geometry</code>.
     *
     * @return
     */
    public int getNumPositions() {
        return getPositions().size();
    }

    public Class<P> getPositionClass() {
        return getPositions().getCoordinateReferenceSystem().getPositionClass();
    }

    /**
     * Returns the position at the specified index in the <code>PointSequence</code> of this <code>Geometry</code>.
     *
     * @param index the position in the <code>PointSequence</code> (first point is at index 0).
     * @return
     */
    public P getPositionN(int index) {
        if (index >= getPositions().size()) {
            throw new IndexOutOfBoundsException();
        }
        double[] coords = new double[getCoordinateDimension()];
        getPositions().getCoordinates(index, coords);
        return Positions.mkPosition(getCoordinateReferenceSystem(), coords);
    }

    /**
     * Returns the <code>PositionSequence</code> of this instance
     *
     * @return
     */
    public PositionSequence<P> getPositions() {
        return this.positions;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !Geometry.class.isAssignableFrom(o.getClass())) return false;
        if (!this.getPositionClass().equals(((Geometry) o).getPositionClass())) return false;
        Geometry<P> otherGeometry = (Geometry<P>) o; //safe cast because we first check for position class equality
        return geomEq.equals(this, otherGeometry);
    }

    /**
     * Returns the {@code Envelope} of this instance.
     * @return the {@code Envelope} of this instance.
     */
    public Envelope<P> getEnvelope() {
        if (isEmpty()) return new Envelope<>(getCoordinateReferenceSystem());
        PositionSequence<P> positions = getPositions();
        EnvelopeVisitor<P> visitor = new EnvelopeVisitor<P>(getCoordinateReferenceSystem());
        positions.accept(visitor);
        return visitor.result();
    }

    @Override
    public int hashCode() {
        int result = getGeometryType().hashCode();
        result = 31 * result + this.getPositions().hashCode();
        return result;
    }

    /**
     * Returns the type of this <code>Geometry</code>.
     *
     * @return the <code>GeometryType</code> of this instance.
     */
    public abstract GeometryType getGeometryType();


    /**
     * Returns the Well-Known Text (WKT) representation of this <code>Geometry</code>.
     *
     * @return
     */
    public String toString() {
        return Wkt.toWkt(this);
    }

    /**
     * Returns the topological dimension of this instance. In non-homogenous collections, this will return the largest
     * topological dimension of the contained <code>Geometries</code>.
     *
     * @return
     */
    public abstract int getDimension();

    /**
     * Accepts a <code>GeometryVisitor</code>.
     * <p>If this <code>Geometry</code> instance is a <code>GeometryCollection</code> then it will pass the
     * visitor to its contained <code>Geometries</code>.</p>
     *
     * @param visitor
     */
    public abstract void accept(GeometryVisitor<P> visitor);

    private static class EnvelopeVisitor<P extends Position> implements PositionVisitor<P> {

            double[] coordinates;
            double xMin = Double.POSITIVE_INFINITY;
            double yMin = Double.POSITIVE_INFINITY;
            double xMax = Double.NEGATIVE_INFINITY;
            double yMax = Double.NEGATIVE_INFINITY;
            final CoordinateReferenceSystem<P> crs;

            EnvelopeVisitor(CoordinateReferenceSystem<P> crs) {
                this.crs = crs;
                coordinates = new double[crs.getCoordinateDimension()];
            }


            @Override
            public void visit(P position) {
                position.toArray(coordinates);
                xMin = Math.min(xMin, coordinates[0]);
                xMax = Math.max(xMax, coordinates[0]);
                yMin = Math.min(yMin, coordinates[1]);
                yMax = Math.max(yMax, coordinates[1]);
            }

            public Envelope<P> result() {
                return new Envelope<P>(xMin, yMin, xMax, yMax, crs);
            }
        }

}

