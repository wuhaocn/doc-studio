import { Node, mergeAttributes } from '@tiptap/core'
import { ReactNodeViewRenderer } from '@tiptap/react'
import DiagramNode from '../components/Editor/DiagramNode'

export default Node.create({
  name: 'diagram',

  addOptions() {
    return {
      HTMLAttributes: {},
    }
  },

  group: 'block',

  addAttributes() {
    return {
      diagramType: {
        default: 'mermaid',
        parseHTML: (element) => {
          const type = element.getAttribute('data-diagram-type')
          // 兼容旧的 drawio 类型，转换为 excalidraw
          return type === 'drawio' ? 'excalidraw' : type
        },
        renderHTML: (attributes) => {
          if (!attributes.diagramType) {
            return {}
          }
          return {
            'data-diagram-type': attributes.diagramType,
          }
        },
      },
      content: {
        default: '',
        parseHTML: (element) => element.getAttribute('data-content'),
        renderHTML: (attributes) => {
          if (!attributes.content) {
            return {}
          }
          return {
            'data-content': attributes.content,
          }
        },
      },
      width: {
        default: '100%',
        parseHTML: (element) => element.getAttribute('data-width'),
        renderHTML: (attributes) => {
          if (!attributes.width) {
            return {}
          }
          return {
            'data-width': attributes.width,
          }
        },
      },
      height: {
        default: '400px',
        parseHTML: (element) => element.getAttribute('data-height'),
        renderHTML: (attributes) => {
          if (!attributes.height) {
            return {}
          }
          return {
            'data-height': attributes.height,
          }
        },
      },
    }
  },

  parseHTML() {
    return [
      {
        tag: 'div[data-type="diagram"]',
        getAttrs: (element) => {
          if (typeof element === 'string') return false
          return {
            diagramType: element.getAttribute('data-diagram-type'),
            content: element.getAttribute('data-content'),
            width: element.getAttribute('data-width'),
            height: element.getAttribute('data-height'),
          }
        },
      },
    ]
  },

  renderHTML({ HTMLAttributes }) {
    return [
      'div',
      mergeAttributes(this.options.HTMLAttributes, HTMLAttributes, {
        'data-type': 'diagram',
        'data-diagram-type': HTMLAttributes.diagramType,
        'data-content': HTMLAttributes.content,
        'data-width': HTMLAttributes.width,
        'data-height': HTMLAttributes.height,
      }),
    ]
  },

  addNodeView() {
    return ReactNodeViewRenderer(DiagramNode, {
      contentDOMElementTag: 'div',
      // 确保传递 getPos 给组件
    })
  },

  addCommands() {
    return {
      insertDiagram:
        (options) =>
        ({ commands }) => {
          return commands.insertContent({
            type: this.name,
            attrs: {
              diagramType: options?.diagramType || 'mermaid',
              content: options?.content || '',
              width: options?.width || '100%',
              height: options?.height || '400px',
            },
          })
        },
      deleteDiagram:
        () =>
        ({ tr, state, dispatch }) => {
          const { selection } = state
          const { $from, $to } = selection
          
          // 查找当前选中的 diagram 节点
          let found = false
          state.doc.nodesBetween($from.pos, $to.pos, (node, pos) => {
            if (node.type.name === this.name && !found) {
              if (dispatch) {
                tr.delete(pos, pos + node.nodeSize)
                dispatch(tr)
              }
              found = true
            }
          })
          
          return found
        },
    }
  },
})

