from kit.art_kit import *

BOA = ('<filter id="plW" x="0" y="0" width="100%" height="100%"><feTurbulence type="fractalNoise" baseFrequency=".42" numOctaves="2" seed="5"/><feColorMatrix values="0 0 0 0 1 0 0 0 0 1 0 0 0 0 1 0 0 0 2.6 -1.2"/><feGaussianBlur stdDeviation=".7"/></filter><filter id="plK" x="0" y="0" width="100%" height="100%"><feTurbulence type="fractalNoise" baseFrequency=".5" numOctaves="2" seed="9"/><feColorMatrix values="0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 2.4 -1.2"/><feGaussianBlur stdDeviation=".8"/></filter><pattern id="boa" width="11" height="10" patternUnits="userSpaceOnUse">'
       '<circle cx="3" cy="3" r="2.6" fill="none" stroke="#fff" stroke-width="1.7" opacity=".3"/>'
       '<circle cx="8.5" cy="8" r="2.6" fill="none" stroke="#fff" stroke-width="1.7" opacity=".3"/>'
       '<path d="M0.5 5.5 a2.6 2.6 0 0 0 5 0 M6 10.5 a2.6 2.6 0 0 0 5 0" fill="none" stroke="#000" stroke-width="1.6" opacity=".3"/></pattern>')


def fleece(color):
    W, H = 400, 350
    left = [(200, 44, 'c'), (164, 40, 'c'), (100, 58, 'c'), (80, 80), (64, 136), (52, 210), (44, 290), (40, 326, 'c'), (96, 334, 'c'), (100, 290),
            (106, 210), (114, 160, 'c'), (114, 250), (114, 328, 'c'), (200, 332, 'c')]
    base = smooth(sym(left, W))
    collar = smooth([(156, 10, 'c'), (200, 6), (244, 10, 'c'), (250, 58, 'c'), (200, 64), (150, 58, 'c')])
    collar_in = 'M160 12 Q200 4 240 12 Q236 26 200 28 Q164 26 160 12 Z'
    pocket = 'M226 120 L282 116 L284 172 Q256 176 228 176 Z'
    flap = 'M224 112 L286 108 L287 128 Q256 132 225 134 Z'
    tex = (BOA + '<g filter="url(#b1)" opacity=".75"><rect width="400" height="350" fill="url(#boa)"/><rect width="400" height="350" fill="url(#boa)" transform="translate(5 4)" opacity=".6"/></g>'
           + '<rect width="400" height="350" filter="url(#plW)" opacity=".5"/><rect width="400" height="350" filter="url(#plK)" opacity=".35"/>')
    fluffy = lambda d, w=8: (f'<path d="{d}" fill="none" stroke="{color}" stroke-width="{w}" stroke-dasharray="0 6" stroke-linecap="round"/>'
                             f'<path d="{d}" fill="none" stroke="#fff" stroke-opacity=".14" stroke-width="{w*.8}" stroke-dasharray="0 6" stroke-linecap="round" transform="translate(-.8 -1)"/>')
    smooth_patch = lambda d, cid: (f'<path d="{d}" fill="#000" opacity=".35" transform="translate(1.5 3)" filter="url(#b3)"/><path d="{d}" fill="{color}"/>'
                                   f'<clipPath id="{cid}"><path d="{d}"/></clipPath><g clip-path="url(#{cid})"><rect width="400" height="350" fill="#000" opacity=".12"/>'
                                   f'{Wt("M232 120 L240 176", .35, 3, 6)}{Wt("M226 114 L286 110", .3, 1, 2)}</g>')
    detail = (fluffy(base)
              + f'<path d="{collar}" fill="#000" opacity=".35" transform="translate(0 4)" filter="url(#b3)"/>' + fluffy(collar, 7)
              + f'<path d="{collar}" fill="{color}"/><clipPath id="flc"><path d="{collar}"/></clipPath><g clip-path="url(#flc)">'
              + tex.replace(BOA, '') + K('M156 48 Q200 58 244 48', .3, 3, 6) + Wt('M166 20 L166 56', .2, 6, 12) + K('M238 16 L238 60', .3, 6, 14) + '</g>'
              + shape(collar_in, '#000', .5) + K(collar_in, .3, 3)
              # nylon chest pocket with zip, nylon binding on hem/cuffs
              + smooth_patch(pocket, 'flp') + smooth_patch(flap, 'flf')
              + stitch('M228 126 L282 122', '#000', .3, 1, '2.5 2') + f'<rect x="268" y="128" width="5" height="12" rx="1.5" fill="#C9CBCE" stroke="#6d6f72" stroke-width=".7"/>'
              + line('M200 10 L200 330', '#000', .45, 7) + '<path d="M200 12 L200 330" stroke="#B9BCC0" stroke-width="3.8" stroke-dasharray="1.6 1.3"/>'
              + '<rect x="196" y="18" width="8" height="13" rx="2" fill="#D4D6D9" stroke="#6d6f72" stroke-width=".8"/>'
              + '<path d="M200 30 L200 50" stroke="#1b1b1b" stroke-width="3" stroke-linecap="round"/><rect x="196.5" y="46" width="7" height="9" rx="2" fill="#1b1b1b"/>')
    bind = lambda d: line(d, '#000', .4, 10) + line(d, '#fff', .14, 3)
    fabric = (bind('M116 324 L284 324') + bind('M42 316 Q68 320 96 326') + bind(f'M{W-42} 316 Q{W-68} 320 {W-96} 326')
              + line('M100 60 Q108 110 114 160', '#000', .3, 2))
    shade = (K('M114 164 Q116 250 114 326', .5, 12, 14) + K(f'M{W-114} 164 Q{W-116} 250 {W-114} 326', .55, 12, 14)
             + K('M84 120 Q70 220 56 316', .25, 12, 12) + K(f'M{W-84} 120 Q{W-70} 220 {W-56} 316', .3, 12, 12)
             + K('M160 200 Q170 260 150 320', .14, 22, 30))
    light = Wt('M150 110 Q160 190 148 270', .26, 22, 44) + Wt('M74 100 Q60 200 48 290', .22, 12, 14)
    return piece(W, H, base, color, (.2, .3), shade, light, tex, detail, fabric=fabric, grain=(1.2, .4), name='fleece')
