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

import com.vividsolutions.jts.algorithm.ConvexHull;
import com.vividsolutions.jts.operation.BoundaryOp;
import com.vividsolutions.jts.operation.IsSimpleOp;
import com.vividsolutions.jts.operation.buffer.BufferOp;
import com.vividsolutions.jts.operation.distance.DistanceOp;
import com.vividsolutions.jts.operation.overlay.OverlayOp;
import com.vividsolutions.jts.operation.overlay.snap.SnapIfNeededOverlayOp;
import com.vividsolutions.jts.operation.relate.RelateOp;
import org.geolatte.geom.crs.CoordinateReferenceSystem;
import org.geolatte.geom.jts.JTS;

/**
 * An implementation of {@code ProjectedGeometryOperations} that delegates to the corresponding JTS operations.
 *
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: 5/3/11
 */
public class JTSGeometryOperations implements ProjectedGeometryOperations {

    private <P extends C2D> boolean envelopeIntersect(Geometry<P> geometry1, Geometry<P> geometry2) {
        return (geometry1.getEnvelope().intersects(geometry2.getEnvelope()));
    }

    /**
     * Throws an <code>IllegalArgumentException</code> when class of parameter is <code>GeometryCollection</code>.
     * Subclasses of <code>GeometryCollection</code> do not trigger the Exception.
     *
     * @param geom
     */
    private <P extends C2D> void checkNotGeometryCollection(Geometry<P> geom) {
        if (GeometryCollection.class.equals(geom.getClass())) {
            throw new IllegalArgumentException("GeometryCollection is not allowed");
        }
    }

    private void checkCompatibleCRS(Geometry<?> geometry, Geometry<?> other) {
        if (!geometry.getCoordinateReferenceSystem().equals(other.getCoordinateReferenceSystem())) {
            throw new IllegalArgumentException("Geometries have different CRS's");
        }
    }

    @Override
    public <P extends C2D> boolean isSimple(final Geometry<P> geometry) {
        return new IsSimpleOp(JTS.to(geometry)).isSimple();
    }

    @Override
    public <P extends C2D> Geometry<P> boundary(final Geometry<P> geometry) {
        final BoundaryOp boundaryOp = new BoundaryOp(JTS.to(geometry));
        final CoordinateReferenceSystem<P> crs = geometry.getCoordinateReferenceSystem();
        return JTS.from(boundaryOp.getBoundary(), crs);
    }

    @Override
    public <P extends C2D> boolean intersects(final Geometry<P> geometry, final Geometry<P> other) {
        if (geometry.isEmpty() || other.isEmpty()) return Boolean.FALSE;
        checkCompatibleCRS(geometry, other);
        if (!envelopeIntersect(geometry, other)) return Boolean.FALSE;
        RelateOp relateOp = new RelateOp(JTS.to(geometry), JTS.to(other));
        return relateOp.getIntersectionMatrix().isIntersects();

    }

    @Override
    public <P extends C2D> boolean touches(final Geometry<P> geometry, final Geometry<P> other) {
        if (geometry.isEmpty() || other.isEmpty()) return Boolean.FALSE;
        checkCompatibleCRS(geometry, other);
        if (!envelopeIntersect(geometry, other)) return Boolean.FALSE;
        final RelateOp relateOp = new RelateOp(JTS.to(geometry), JTS.to(other));
        return relateOp.getIntersectionMatrix().isTouches(geometry.getDimension(), other.getDimension());

    }

    @Override
    public <P extends C2D> boolean crosses(final Geometry<P> geometry, final Geometry<P> other) {
        if (geometry.isEmpty() || other.isEmpty()) return Boolean.FALSE;
        checkCompatibleCRS(geometry, other);
        if (!envelopeIntersect(geometry, other)) return Boolean.FALSE;
        final RelateOp relateOp = new RelateOp(JTS.to(geometry), JTS.to(other));
        return relateOp.getIntersectionMatrix().isCrosses(geometry.getDimension(), other.getDimension());
    }

    @Override
    public <P extends C2D> boolean contains(final Geometry<P> geometry, final Geometry<P> other) {
        if (geometry.isEmpty() || other.isEmpty()) return Boolean.FALSE;
        checkCompatibleCRS(geometry, other);
        if (!geometry.getEnvelope().contains(other.getEnvelope())) return Boolean.FALSE;
        final RelateOp relateOp = new RelateOp(JTS.to(geometry), JTS.to(other));
        return relateOp.getIntersectionMatrix().isContains();
    }

    @Override
    public <P extends C2D> boolean overlaps(final Geometry<P> geometry, final Geometry<P> other) {
        if (geometry.isEmpty() || other.isEmpty()) return Boolean.FALSE;
        checkCompatibleCRS(geometry, other);
        if (!envelopeIntersect(geometry, other)) return Boolean.FALSE;
        final RelateOp relateOp = new RelateOp(JTS.to(geometry), JTS.to(other));
        return relateOp.getIntersectionMatrix().isOverlaps(geometry.getDimension(), other.getDimension());
    }

    @Override
    public <P extends C2D> boolean relates(final Geometry<P> geometry, final Geometry<P> other, final String matrix) {
        if (geometry.isEmpty() || other.isEmpty()) return Boolean.FALSE;
        checkCompatibleCRS(geometry, other);
        final RelateOp relateOp = new RelateOp(JTS.to(geometry), JTS.to(other));
        return relateOp.getIntersectionMatrix().matches(matrix);
    }


    @Override
    public <P extends C2D> double distance(final Geometry<P> geometry, final Geometry<P> other) {
        final DistanceOp op = new DistanceOp(JTS.to(geometry), JTS.to(other));
        return op.distance();
    }

    @Override
    public <P extends C2D> Geometry<P> buffer(final Geometry<P> geometry, final double distance) {
        final BufferOp op = new BufferOp(JTS.to(geometry));
        return JTS.from(op.getResultGeometry(distance), geometry.getCoordinateReferenceSystem());
    }

    @Override
    public <P extends C2D> Geometry<P> convexHull(final Geometry<P> geometry) {
        final ConvexHull convexHull = new ConvexHull(JTS.to(geometry));
        return JTS.from(convexHull.getConvexHull(), geometry.getCoordinateReferenceSystem());

    }

    @Override
    public <P extends C2D> Geometry<P> intersection(final Geometry<P> geometry, final Geometry<P> other) {
        checkCompatibleCRS(geometry, other);
        if (geometry.isEmpty() || other.isEmpty()) return new Point<P>(geometry.getCoordinateReferenceSystem());
        checkNotGeometryCollection(geometry);
        checkNotGeometryCollection(other);
        com.vividsolutions.jts.geom.Geometry intersection = SnapIfNeededOverlayOp.overlayOp(JTS.to(geometry), JTS.to(other), OverlayOp.INTERSECTION);
        return JTS.from(intersection, geometry.getCoordinateReferenceSystem());
    }

    @Override
    public <P extends C2D> Geometry<P> union(final Geometry<P> geometry, final Geometry<P> other) {
        checkCompatibleCRS(geometry, other);
        if (geometry.isEmpty()) return other;
        if (other.isEmpty()) return geometry;
        checkNotGeometryCollection(geometry);
        checkNotGeometryCollection(other);
        com.vividsolutions.jts.geom.Geometry union = SnapIfNeededOverlayOp.overlayOp(JTS.to(geometry), JTS.to(other), OverlayOp.UNION);
        return JTS.from(union, geometry.getCoordinateReferenceSystem());

    }

    @Override
    public <P extends C2D> Geometry<P> difference(final Geometry<P> geometry, final Geometry<P> other) {
        checkCompatibleCRS(geometry, other);
        if (geometry.isEmpty()) return new Point<P>(geometry.getCoordinateReferenceSystem());
        if (other.isEmpty()) return geometry;
        checkNotGeometryCollection(geometry);
        checkNotGeometryCollection(other);
        com.vividsolutions.jts.geom.Geometry difference = SnapIfNeededOverlayOp.overlayOp(JTS.to(geometry), JTS.to(other), OverlayOp.DIFFERENCE);
        return JTS.from(difference, geometry.getCoordinateReferenceSystem());

    }

    @Override
    public  <P extends C2D> Geometry<P> symmetricDifference(final Geometry<P> geometry, final Geometry<P> other) {
                checkCompatibleCRS(geometry, other);
                if (geometry.isEmpty()) return other;
                if (other.isEmpty()) return geometry;
                checkNotGeometryCollection(geometry);
                checkNotGeometryCollection(other);
                com.vividsolutions.jts.geom.Geometry symDifference = SnapIfNeededOverlayOp.overlayOp(JTS.to(geometry), JTS.to(other), OverlayOp.SYMDIFFERENCE);
                return JTS.from(symDifference, geometry.getCoordinateReferenceSystem());
    }

    @Override
    public <P extends C2D, G extends Geometry<P> & Linear<P>> double length(final G geometry) {
                return JTS.to(geometry).getLength();

    }

    @Override
    public  <P extends C2D, G extends Geometry<P> & Polygonal<P>> double area(final G geometry) {
                return JTS.to(geometry).getArea();

    }

    @Override
    @SuppressWarnings("unchecked")
    public  <P extends C2D, G extends Geometry<P> & Polygonal<P>> Point<P> centroid(final G geometry) {
                return (Point<P>) JTS.from(JTS.to(geometry).getCentroid());

    }


}
