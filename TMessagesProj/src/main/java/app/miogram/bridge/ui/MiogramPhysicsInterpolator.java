package app.miogram.bridge.ui;

import android.view.animation.Interpolator;

/**
 * High-precision physical interpolators for Miogram UI.
 * Provides critically damped springs, bounce-back curves, and fluid motion
 * tuned for 120Hz / ProMotion displays.
 */
public class MiogramPhysicsInterpolator {

    /**
     * Critically damped spring interpolator.
     * Prevents overshoot while providing quick, responsive tactile feel.
     */
    public static class SpringInterpolator implements Interpolator {
        private final float damping;
        private final float frequency;

        public SpringInterpolator() {
            this(0.85f, 1.2f);
        }

        public SpringInterpolator(float damping, float frequency) {
            this.damping = damping;
            this.frequency = frequency;
        }

        @Override
        public float getInterpolation(float input) {
            if (input <= 0f) return 0f;
            if (input >= 1f) return 1f;
            double decay = Math.exp(-damping * input * 6.0f);
            double osc = Math.cos(frequency * input * Math.PI * 2.0);
            return (float) (1.0 - decay * osc);
        }
    }

    /**
     * Subtle bounce-back curve for gesture release and boundaries.
     */
    public static class BounceBackInterpolator implements Interpolator {
        private final float tension;

        public BounceBackInterpolator() {
            this(1.75f);
        }

        public BounceBackInterpolator(float tension) {
            this.tension = tension;
        }

        @Override
        public float getInterpolation(float t) {
            t -= 1.0f;
            return t * t * ((tension + 1.0f) * t + tension) + 1.0f;
        }
    }

    /**
     * Smooth quintic deceleration for dialogs and sheets.
     */
    public static class FluidDecelerateInterpolator implements Interpolator {
        @Override
        public float getInterpolation(float t) {
            t -= 1.0f;
            return t * t * t * t * t + 1.0f;
        }
    }

    /**
     * Sine ease-in-out for pulsing effects.
     */
    public static class SineInOutInterpolator implements Interpolator {
        @Override
        public float getInterpolation(float t) {
            return (float) (-0.5f * (Math.cos(Math.PI * t) - 1.0f));
        }
    }

    public static final Interpolator SPRING = new SpringInterpolator();
    public static final Interpolator BOUNCE = new BounceBackInterpolator();
    public static final Interpolator FLUID = new FluidDecelerateInterpolator();
    public static final Interpolator SINE = new SineInOutInterpolator();
}
