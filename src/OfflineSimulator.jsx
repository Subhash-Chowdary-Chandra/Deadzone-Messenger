import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { SignalLow, BatteryMedium, AlertCircle, PhoneMissed, Loader, AlertTriangle, Image as ImageIcon } from 'lucide-react';

export default function OfflineSimulator() {
  const [scenario, setScenario] = useState(0);

  useEffect(() => {
    const timer = setInterval(() => {
      setScenario((prev) => (prev + 1) % 3);
    }, 4000);
    return () => clearInterval(timer);
  }, []);

  return (
    <div style={{
      width: '260px',
      height: '460px',
      background: '#000',
      borderRadius: '30px',
      border: '8px solid #2a2a35',
      position: 'relative',
      overflow: 'hidden',
      display: 'flex',
      flexDirection: 'column',
      boxShadow: '0 20px 40px rgba(0,0,0,0.8), 0 0 20px rgba(255,0,0,0.2)'
    }}>
      {/* Notch */}
      <div style={{ position: 'absolute', top: 0, left: '50%', transform: 'translateX(-50%)', width: '100px', height: '18px', background: '#2a2a35', borderBottomLeftRadius: '10px', borderBottomRightRadius: '10px', zIndex: 10 }} />
      
      {/* Status Bar */}
      <div style={{ padding: '8px 15px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', fontSize: '0.7rem', color: 'gray', zIndex: 5, paddingTop: '10px' }}>
        <span>10:42</span>
        <div style={{ display: 'flex', gap: '5px', alignItems: 'center' }}>
          <span style={{ color: 'red', fontWeight: 'bold' }}>No Service</span>
          <BatteryMedium size={14} color="gray" />
        </div>
      </div>

      {/* Screen Content */}
      <div style={{ flex: 1, position: 'relative', background: '#0f1015' }}>
        <AnimatePresence mode="wait">
          
          {/* Scenario 0: Messaging Fails */}
          {scenario === 0 && (
            <motion.div 
              key="messaging"
              initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
              style={{ padding: '15px', display: 'flex', flexDirection: 'column', height: '100%' }}
            >
              <div style={{ borderBottom: '1px solid rgba(255,255,255,0.05)', paddingBottom: '10px', marginBottom: '20px', textAlign: 'center', fontWeight: 'bold' }}>Family Group</div>
              
              <div style={{ alignSelf: 'flex-start', background: '#2a2a35', padding: '10px', borderRadius: '15px', borderBottomLeftRadius: '0', maxWidth: '80%', marginBottom: '10px' }}>
                Where are you guys?
              </div>
              
              <div style={{ alignSelf: 'flex-end', display: 'flex', flexDirection: 'column', alignItems: 'flex-end', marginBottom: '10px' }}>
                <div style={{ background: '#0d47a1', padding: '10px', borderRadius: '15px', borderBottomRightRadius: '0', maxWidth: '80%' }}>
                  We are trapped here.
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '5px', color: 'red', fontSize: '0.75rem', marginTop: '5px' }}>
                  <AlertCircle size={12} /> Not Delivered
                </div>
              </div>

              <div style={{ alignSelf: 'flex-end', display: 'flex', flexDirection: 'column', alignItems: 'flex-end' }}>
                <div style={{ background: '#0d47a1', padding: '10px', borderRadius: '15px', borderBottomRightRadius: '0', maxWidth: '80%' }}>
                  Can anyone hear me?
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '5px', color: 'red', fontSize: '0.75rem', marginTop: '5px' }}>
                  <AlertCircle size={12} /> Not Delivered
                </div>
              </div>
            </motion.div>
          )}

          {/* Scenario 1: Call Fails */}
          {scenario === 1 && (
            <motion.div 
              key="call"
              initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
              style={{ display: 'flex', flexDirection: 'column', height: '100%', alignItems: 'center', justifyContent: 'center', background: '#1c0505' }}
            >
              <div style={{ width: '80px', height: '80px', borderRadius: '50%', background: 'gray', display: 'flex', alignItems: 'center', justifyContent: 'center', marginBottom: '20px' }}>
                <span style={{ fontSize: '2rem' }}>👤</span>
              </div>
              <h3 style={{ marginBottom: '5px' }}>Mom</h3>
              <p style={{ color: 'red', marginBottom: '40px' }}>Call Failed</p>
              
              <div style={{ width: '60px', height: '60px', borderRadius: '50%', background: 'red', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                <PhoneMissed size={28} color="white" />
              </div>
            </motion.div>
          )}

          {/* Scenario 2: Social Media Fails */}
          {scenario === 2 && (
            <motion.div 
              key="social"
              initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
              style={{ display: 'flex', flexDirection: 'column', height: '100%', background: '#0f1015' }}
            >
              <div style={{ padding: '15px', borderBottom: '1px solid rgba(255,255,255,0.05)', fontWeight: 'bold' }}>Feed</div>
              
              <div style={{ padding: '15px' }}>
                <div style={{ display: 'flex', gap: '10px', marginBottom: '10px' }}>
                  <div style={{ width: '30px', height: '30px', borderRadius: '50%', background: '#2a2a35' }} />
                  <div style={{ flex: 1, height: '15px', background: '#2a2a35', borderRadius: '4px', marginTop: '5px' }} />
                </div>
                <div style={{ width: '100%', height: '150px', background: '#1c1d25', borderRadius: '10px', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                  <motion.div animate={{ rotate: 360 }} transition={{ repeat: Infinity, duration: 1, ease: 'linear' }}>
                    <Loader size={24} color="gray" />
                  </motion.div>
                </div>
              </div>

              {/* Toast Error */}
              <motion.div 
                initial={{ y: 50, opacity: 0 }} animate={{ y: 0, opacity: 1 }}
                style={{ position: 'absolute', bottom: '20px', left: '10px', right: '10px', background: 'rgba(255,0,0,0.8)', padding: '10px', borderRadius: '8px', display: 'flex', alignItems: 'center', gap: '10px', fontSize: '0.8rem' }}
              >
                <AlertTriangle size={16} /> No Internet Connection
              </motion.div>

            </motion.div>
          )}

        </AnimatePresence>
      </div>

    </div>
  );
}
