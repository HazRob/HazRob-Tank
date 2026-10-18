package com.servoc.app.measurements;

/** Reference comparisons must preserve measurement duration, calibration and context. */
public final class PhysicalReferencePolicy {
    private PhysicalReferencePolicy(){}
    public static final class Result {
        public final boolean exceeded;
        public final String explanation;
        Result(boolean exceeded,String explanation){this.exceeded=exceeded;this.explanation=explanation;}
    }
    public static Result evaluate(int index,double value,boolean valid,boolean uvIndexValidated,
                                  boolean occupiedIndoorSpace,long averagingSeconds,boolean certifiedMetric){
        if(!valid||!Double.isFinite(value))return new Result(false,"قراءة غير متاحة");
        switch(index){
            case 1:
                if(!occupiedIndoorSpace)return new Result(false,"ASHRAE 62.1 • يتطلب سياق مكان داخلي مشغول");
                return new Result(value>=65.0,"ASHRAE 62.1 • توصية ضبط الرطوبة الداخلية دون 65%");
            case 8:
                if(!uvIndexValidated)return new Result(false,"WHO • مؤشر UV غير معتمد القياس");
                return new Result(value>=3.0,"WHO • الحماية من الشمس موصى بها عند UVI ≥ 3");
            case 10:
            case 11:
                if(!certifiedMetric||averagingSeconds<86400L)return new Result(false,"WHO 2021 • يلزم متوسط 24 ساعة وقياس جسيمات موثوق");
                double limit=index==10?15.0:45.0;
                return new Result(value>limit,"WHO 2021 • متوسط 24 ساعة "+(index==10?"PM2.5 ≤ 15":"PM10 ≤ 45")+" µg/m³");
            case 5:
                return new Result(false,"OSHA 1910.95 • يلزم dBA معاير وجرعة تعرض زمنية، وليس dB SPL لحظياً");
            case 0:
                return new Result(false,"ASHRAE 55 • الراحة الحرارية تعتمد على ظروف متعددة");
            default:
                return new Result(false,"لا يوجد حد عام قابل للتطبيق على قراءة لحظية منفردة");
        }
    }
}
