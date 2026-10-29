package com.hazrob.tank.chemcore.v6;

import java.util.Arrays;

/**
 * Research-stage N03-G for 08.1/08.2/08.3/08.4. Four REAL numerical
 * architecture operations with FIXED, documented encoder parameters and a
 * TRAINED binary logistic readout. The convolution filters, recurrent gates
 * and graph propagation weights are NOT end-to-end trained.
 *
 * Sequence representation: time-major [time][source sensor channel];
 * no headspace food data, no prior gas-engine dependencies, no Tank ppm.
 * Times must be equally spaced by caller; temporal samples from one exposure
 * are NEVER independent ML training rows.
 */
public final class TemporalNeuralResearchV6 {
    private TemporalNeuralResearchV6(){}
    public enum Kind {CNN1D_FROZEN_FILTERS, TCN_CAUSAL_FROZEN_FILTERS,
        GRU_FROZEN_GATES, GNN_FROZEN_MESSAGE_PASSING}
    private static void require(boolean b,String message){
        if(!b)throw new IllegalArgumentException(message);
    }
    private static int check(double[][] sequence){
        require(sequence!=null&&sequence.length>=8,"minimum eight time samples");
        require(sequence[0]!=null&&sequence[0].length>0&&
                sequence[0].length<=32,"1..32 sensor channels");
        int d=sequence[0].length;
        for(double[] row:sequence){
            require(row!=null&&row.length==d,"all samples share sensor axes");
            for(double x:row)require(Double.isFinite(x),"undefined source signal");
        }
        return d;
    }
    private static void graph(boolean[][] adj,int channels){
        require(adj!=null&&adj.length==channels,"source-qualified graph dimension");
        for(int i=0;i<channels;i++){
            require(adj[i]!=null&&adj[i].length==channels,
                    "graph square adjacency required");
            require(!adj[i][i],"self-edge is handled internally, explicit diagonal disallowed");
            for(int j=0;j<i;j++)
                require(adj[i][j]==adj[j][i],"only undirected sensor graph supported");
        }
    }
    private static double sigmoid(double z){
        return z>=0?1/(1+Math.exp(-z)):Math.exp(z)/(1+Math.exp(z));
    }
    private static double[] cnn(double[][] s){
        int d=check(s),t=s.length;
        // Four independently specified temporal filters, width 3:
        // rising edge, falling edge, short average, centered curvature.
        double[][] k={{-.5,0,.5},{.5,0,-.5},
                       {1.0/3,1.0/3,1.0/3},{.5,-1,.5}};
        double[] out=new double[d*k.length];
        for(int channel=0;channel<d;channel++){
            for(int kernel=0;kernel<k.length;kernel++){
                double sum=0;
                for(int time=2;time<t;time++){
                    double z=0;
                    for(int j=0;j<3;j++)
                        z+=k[kernel][j]*s[time-2+j][channel];
                    sum+=Math.max(0,z);
                }
                out[channel*k.length+kernel]=sum/(t-2);
            }
        }
        return out;
    }
    /** True causal dilated convolutions at dilation 1, 2, 4. The output at
     * prefixTime only depends on source samples at indices <= prefixTime.
     */
    public static double[] causalTcnPrefix(double[][] s,int prefixTime){
        int d=check(s);
        require(prefixTime>=0&&prefixTime<s.length,"causal index bounds");
        double[] result=new double[d*3];
        for(int channel=0;channel<d;channel++){
            double[] previous=new double[prefixTime+1];
            for(int time=0;time<=prefixTime;time++)
                previous[time]=s[time][channel];
            for(int layer=0;layer<3;layer++){
                int dilation=1<<layer;
                double[] current=new double[prefixTime+1];
                for(int time=0;time<=prefixTime;time++){
                    double z=.5*previous[time];
                    if(time>=dilation)z+=.35*previous[time-dilation];
                    if(time>=2*dilation)z+=.15*previous[time-2*dilation];
                    current[time]=Math.max(0,z);
                }
                result[channel*3+layer]=current[prefixTime];
                previous=current;
            }
        }
        return result;
    }
    private static double[] tcn(double[][] s){
        int d=check(s),t=s.length;
        double[] result=new double[d*6]; // terminal and average layer outputs
        for(int channel=0;channel<d;channel++){
            double[] previous=new double[t];
            for(int time=0;time<t;time++)previous[time]=s[time][channel];
            for(int layer=0;layer<3;layer++){
                int dilation=1<<layer;
                double[] current=new double[t];double mean=0;
                for(int time=0;time<t;time++){
                    double z=.5*previous[time];
                    if(time>=dilation)z+=.35*previous[time-dilation];
                    if(time>=2*dilation)z+=.15*previous[time-2*dilation];
                    current[time]=Math.max(0,z);mean+=current[time]/t;
                }
                result[channel*6+layer*2]=current[t-1];
                result[channel*6+layer*2+1]=mean;
                previous=current;
            }
        }
        return result;
    }
    /** Gate equations of a scalar/channelwise GRU with FIXED coefficients:
     * z_t=sigmoid(.8*x_t+.2*h), r_t=sigmoid(-.3*x_t+.4*h),
     * n_t=tanh(.8*x_t+.5*r_t*h), h_t=(1-z_t)*h + z_t*n_t.
     * No backpropagation through time and no cross-channel recurrent matrices.
     */
    public static double[] gruPrefix(double[][] s,int prefixTime){
        int d=check(s);
        require(prefixTime>=0&&prefixTime<s.length,"GRU prefix index bounds");
        double[] hidden=new double[d];
        for(int time=0;time<=prefixTime;time++)
            for(int channel=0;channel<d;channel++){
                double x=s[time][channel],h=hidden[channel];
                double z=sigmoid(.8*x+.2*h);
                double r=sigmoid(-.3*x+.4*h);
                double proposal=Math.tanh(.8*x+.5*r*h);
                hidden[channel]=(1-z)*h+z*proposal;
            }
        return hidden;
    }
    private static double[] gru(double[][] s){
        int d=check(s);double[] hidden=new double[d],avg=new double[d];
        for(int time=0;time<s.length;time++){
            for(int channel=0;channel<d;channel++){
                double x=s[time][channel],h=hidden[channel];
                double z=sigmoid(.8*x+.2*h);
                double r=sigmoid(-.3*x+.4*h);
                double proposal=Math.tanh(.8*x+.5*r*h);
                hidden[channel]=(1-z)*h+z*proposal;
                avg[channel]+=hidden[channel]/s.length;
            }
        }
        double[] out=new double[2*d];
        for(int i=0;i<d;i++){out[i*2]=hidden[i];out[i*2+1]=avg[i];}
        return out;
    }
    /** Two genuine graph message-passing hops on a caller-provided undirected
     * sensor graph. Graph topology is scientific metadata, NOT inferred
     * from the source UCI file or arbitrary hardware proximity.
     */
    private static double[] gnn(double[][] s,boolean[][] adjacency){
        int d=check(s);graph(adjacency,d);
        double[] h=new double[d],out=new double[2*d];
        for(int channel=0;channel<d;channel++){
            double mean=0;
            for(double[] row:s)mean+=row[channel]/s.length;
            h[channel]=Math.tanh(mean);
        }
        for(int hop=0;hop<2;hop++){
            double[] next=new double[d];
            for(int channel=0;channel<d;channel++){
                double neighbors=0;int count=0;
                for(int j=0;j<d;j++)if(adjacency[channel][j]){
                    neighbors+=h[j];count++;
                }
                // Isolated nodes: self-state only; no fabricated neighbor.
                next[channel]=Math.tanh(.6*h[channel]+
                    (count>0?.4*neighbors/count:0));
                out[channel*2+hop]=next[channel];
            }
            h=next;
        }
        return out;
    }
    public static double[] encode(Kind kind,double[][] s,boolean[][] adjacency){
        require(kind!=null,"registered neural research method required");
        switch(kind){
            case CNN1D_FROZEN_FILTERS:return cnn(s);
            case TCN_CAUSAL_FROZEN_FILTERS:return tcn(s);
            case GRU_FROZEN_GATES:return gru(s);
            case GNN_FROZEN_MESSAGE_PASSING:return gnn(s,adjacency);
            default:throw new IllegalArgumentException("unsupported research method");
        }
    }

    /** Binary logistic readout fitted to original source EXPOSURE rows.
     * All featurizer weights frozen. The caller owns train/calibration/test
     * separation, original source labels and acquisition metadata.
     */
    public static final class Model {
        public final Kind kind;
        public final int sourceTrainingExposures,featureCount,iterations;
        public final boolean encoderWeightsTrained,probabilitiesCalibrated,
                             physicalTankGasIdentityValidated;
        private final double[] mean,scale,weight;
        private final double bias;
        private final boolean[][] graph;
        private Model(Kind kind,int n,int iterations,double[] mean,double[] scale,
                      double[] weight,double bias,boolean[][] adjacency){
            this.kind=kind;sourceTrainingExposures=n;featureCount=weight.length;
            this.iterations=iterations;this.mean=mean;this.scale=scale;
            this.weight=weight;this.bias=bias;
            if(adjacency==null)graph=null;
            else{
                graph=new boolean[adjacency.length][];
                for(int i=0;i<adjacency.length;i++)graph[i]=adjacency[i].clone();
            }
            encoderWeightsTrained=false;
            probabilitiesCalibrated=false;
            physicalTankGasIdentityValidated=false;
        }
        public double sourceRelativeScore(double[][] s){
            double[] feature=encode(kind,s,graph);
            require(feature.length==featureCount,"sequence feature dimension");
            double z=bias;
            for(int j=0;j<featureCount;j++)
                z+=weight[j]*(feature[j]-mean[j])/scale[j];
            require(Double.isFinite(z),"source classifier score overflow");
            return sigmoid(z);
        }
    }
    public static Model fitBinary(Kind kind,double[][][] sourceExposures,
                 int[] binaryLabels,boolean[][] adjacency,int iterations,
                 double learningRate,double l2){
        require(kind!=null&&sourceExposures!=null&&binaryLabels!=null&&
            sourceExposures.length>=4&&sourceExposures.length==binaryLabels.length&&
            iterations>=1&&iterations<=3000&&
            Double.isFinite(learningRate)&&learningRate>0&&learningRate<=1&&
            Double.isFinite(l2)&&l2>=0,"source-only logistic head training specification");
        int n=sourceExposures.length,d=check(sourceExposures[0]);
        if(kind==Kind.GNN_FROZEN_MESSAGE_PASSING)graph(adjacency,d);
        double[][] features=new double[n][];int zeros=0,ones=0;
        for(int i=0;i<n;i++){
            require(check(sourceExposures[i])==d,"all original exposure sensor axes");
            if(binaryLabels[i]==1)ones++;
            else if(binaryLabels[i]==0)zeros++;
            else throw new IllegalArgumentException("binary label 0 or 1 required");
            features[i]=encode(kind,sourceExposures[i],adjacency);
        }
        require(zeros>0&&ones>0,"independent source observations from both classes");
        int p=features[0].length;
        double[] mean=new double[p],scale=new double[p];
        for(double[] f:features)for(int j=0;j<p;j++)mean[j]+=f[j]/n;
        for(double[] f:features)for(int j=0;j<p;j++){
            double e=f[j]-mean[j];scale[j]+=e*e/n;
        }
        for(int j=0;j<p;j++){
            scale[j]=Math.sqrt(scale[j]);
            if(scale[j]<1e-10)scale[j]=1; // known constant, not invented variance
        }
        double[] weight=new double[p];double bias=0;
        for(int step=0;step<iterations;step++){
            double[] grad=new double[p];double gradB=0;
            for(int i=0;i<n;i++){
                double z=bias;
                for(int j=0;j<p;j++)z+=weight[j]*(features[i][j]-mean[j])/scale[j];
                double delta=sigmoid(z)-binaryLabels[i];
                gradB+=delta/n;
                for(int j=0;j<p;j++)grad[j]+=
                    delta*(features[i][j]-mean[j])/scale[j]/n;
            }
            for(int j=0;j<p;j++)weight[j]-=learningRate*(grad[j]+l2*weight[j]);
            bias-=learningRate*gradB;
        }
        for(double z:weight)require(Double.isFinite(z),"undefined neural readout");
        require(Double.isFinite(bias),"undefined neural readout intercept");
        return new Model(kind,n,iterations,mean,scale,weight,bias,adjacency);
    }
}
